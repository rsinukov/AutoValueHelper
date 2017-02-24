package org.jetbrains.plugins.autovaluehelper;

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

class AutoValueBuilderGenerator implements Runnable {

    private static final String BUILDER_CLASS_NAME = "Builder";
    private static final String NONNULL = "android.support.annotation.NonNull";
    private static final String NULLABLE = "android.support.annotation.Nullable";
    private static final String AUTO_VALUE_BUILDER = "com.google.auto.value.AutoValue.Builder";

    private final Project project;
    private final PsiFile file;
    private final Editor editor;
    private final List<PsiMethodMember> selectedMethods;
    private final PsiElementFactory psiElementFactory;

    static void generate(@NotNull final Project project,
                         @NotNull final Editor editor,
                         @NotNull final PsiFile file,
                         @NotNull final List<PsiMethodMember> selectedMethods
    ) {
        final Runnable builderGenerator = new AutoValueBuilderGenerator(project, file, editor, selectedMethods);
        ApplicationManager.getApplication().runWriteAction(builderGenerator);
    }

    private AutoValueBuilderGenerator(
            @NotNull final Project project,
            @NotNull final PsiFile file,
            @NotNull final Editor editor,
            @NotNull final List<PsiMethodMember> selectedMethods
    ) {
        this.project = project;
        this.file = file;
        this.editor = editor;
        this.selectedMethods = selectedMethods;
        this.psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    @Override
    public void run() {
        final PsiClass targetClass = AutoValueUtils.getStaticOrTopLevelClass(file, editor);
        if (targetClass == null) {
            return;
        }
        final PsiClass builderClass = findOrCreateBuilderClass(targetClass);
        final PsiType builderType = psiElementFactory.createTypeFromText(BUILDER_CLASS_NAME, null);

        // generate builder() method
        final PsiMethod newBuilderMethod = generateBuilderMethod(targetClass, builderType);
        addMethod(targetClass, null, newBuilderMethod);

        // builder methods
        PsiElement lastAddedElement = null;
        for (final PsiMethodMember member : selectedMethods) {
            final PsiMethod setterMethod = generateBuilderSetter(builderType, member);
            lastAddedElement = addMethod(builderClass, lastAddedElement, setterMethod);
        }

        // builder.build() method
        final PsiMethod buildMethod = generateBuildMethod(targetClass);
        addMethod(builderClass, lastAddedElement, buildMethod);

        deleteUnusedMethods(builderClass);

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(file);
        CodeStyleManager.getInstance(project).reformat(builderClass);
    }

    private PsiMethod generateBuilderMethod(@NotNull PsiClass targetClass, @NotNull PsiType builderType) {
        final PsiMethod newBuilderMethod = psiElementFactory.createMethod("builder", builderType);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.STATIC, true);
        PsiUtil.setModifierProperty(newBuilderMethod, PsiModifier.PUBLIC, true);
        newBuilderMethod.getModifierList().addAnnotation(NONNULL);

        final PsiCodeBlock builderMethodBody = newBuilderMethod.getBody();
        if (builderMethodBody != null) {
            StringBuilder classNameBuilder = new StringBuilder();
            PsiClass currentClass = targetClass;
            while (currentClass != null) {
                final String currentClassName = currentClass.getName();
                classNameBuilder.insert(0, currentClassName).insert(0, "_");
                currentClass = currentClass.getContainingClass();
            }
            classNameBuilder.insert(0, "AutoValue");

            final PsiStatement returnStatement = psiElementFactory.createStatementFromText(
                    String.format("return new %s.%s();", classNameBuilder.toString(), BUILDER_CLASS_NAME),
                    newBuilderMethod
            );
            builderMethodBody.add(returnStatement);
        }
        return newBuilderMethod;
    }

    @NotNull
    private PsiMethod generateBuilderSetter(@NotNull final PsiType builderType, @NotNull final PsiMethodMember member) {
        final PsiMethod originalMethod = member.getElement();
        final PsiType parameterType = originalMethod.getReturnType();
        final String methodName = originalMethod.getName();
        final String parameterName = methodName;
        assert parameterType != null;

        final PsiMethod setterMethod = psiElementFactory.createMethod(methodName, builderType);
        setterMethod.getModifierList().addAnnotation(NONNULL);

        setterMethod.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        setterMethod.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);

        final PsiParameter setterParameter = psiElementFactory.createParameter(parameterName, parameterType);
        if (!(parameterType instanceof PsiPrimitiveType)) {
            if (originalMethod.getModifierList().findAnnotation(NULLABLE) != null) {
                setterParameter.getModifierList().addAnnotation(NULLABLE);
            } else if (originalMethod.getModifierList().findAnnotation(NONNULL) != null) {
                setterParameter.getModifierList().addAnnotation(NONNULL);
            }
        }
        setterMethod.getParameterList().add(setterParameter);
        setterMethod.getBody().delete();

        return setterMethod;
    }

    @NotNull
    private PsiMethod generateBuildMethod(@NotNull final PsiClass targetClass) {
        final PsiType targetClassType = psiElementFactory.createType(targetClass);
        final PsiMethod buildMethod = psiElementFactory.createMethod("build", targetClassType);

        buildMethod.getModifierList().addAnnotation(NONNULL);
        buildMethod.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        buildMethod.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);
        buildMethod.getBody().delete();

        return buildMethod;
    }

    @NotNull
    private PsiClass findOrCreateBuilderClass(final PsiClass targetClass) {
        final PsiClass builderClass = targetClass.findInnerClassByName(BUILDER_CLASS_NAME, false);
        if (builderClass == null) {
            return createBuilderClass(targetClass);
        }

        return builderClass;
    }

    @NotNull
    private PsiClass createBuilderClass(final PsiClass targetClass) {
        final PsiClass builderClass = (PsiClass) targetClass.add(psiElementFactory.createClass(BUILDER_CLASS_NAME));
        builderClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        builderClass.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);
        builderClass.getModifierList().addAnnotation(AUTO_VALUE_BUILDER);
        return builderClass;
    }

    @NotNull
    private PsiElement addMethod(
            @NotNull final PsiClass target,
            @Nullable final PsiElement after,
            @NotNull final PsiMethod newMethod
    ) {
        PsiMethod existingMethod = target.findMethodBySignature(newMethod, false);

        if (existingMethod == null) {
            if (after != null) {
                return target.addAfter(newMethod, after);
            } else {
                return target.add(newMethod);
            }
        }

        existingMethod.replace(newMethod);
        return existingMethod;
    }

    private void deleteUnusedMethods(@NotNull PsiClass builderClass) {
        final Set<String> methodNamesToStore = selectedMethods.stream()
                .map(methodMember -> ((PsiMethod) methodMember.getPsiElement()).getName())
                .collect(Collectors.toSet());
        for (PsiMethod method : builderClass.getAllMethods()) {
            if (!methodNamesToStore.contains(method.getName())) {
                method.delete();
            }
        }
    }
}
