package org.jetbrains.plugins.autovaluehelper.builder;

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.autovaluehelper.AutoValueBaseHelperGenerator;
import org.jetbrains.plugins.autovaluehelper.AutoValueUtils;

import java.util.*;
import java.util.stream.Collectors;

class AutoValueBuilderGenerator extends AutoValueBaseHelperGenerator {

    private static final String BUILDER_CLASS_NAME = "Builder";
    public static final String BUILD_METHOD_NAME = "build";

    private final PsiFile file;
    private final Editor editor;

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
        super(selectedMethods, project);
        this.file = file;
        this.editor = editor;
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
        for (final PsiMethodMember member : selectedMethods) {
            final PsiMethod setterMethod = generateBuilderSetter(builderType, member);
            addMethod(builderClass, null, setterMethod);
        }

        // builder.build() method
        final PsiMethod buildMethod = generateBuildMethod(targetClass);
        addMethod(builderClass, null, buildMethod);

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
            final String className = getAutoValueClassName(targetClass);

            final PsiStatement returnStatement = psiElementFactory.createStatementFromText(
                    String.format("return new %s.%s();", className, BUILDER_CLASS_NAME),
                    newBuilderMethod
            );
            builderMethodBody.add(returnStatement);
        }
        return newBuilderMethod;
    }

    @NotNull
    private PsiMethod generateBuilderSetter(
            @NotNull final PsiType builderType,
            @NotNull final PsiMethodMember getterMember
    ) {
        final PsiMethod getterMethod = getterMember.getElement();
        final PsiType parameterType = getterMethod.getReturnType();
        final String methodName = getterMethod.getName();
        assert parameterType != null;

        final PsiMethod setterMethod = psiElementFactory.createMethod(methodName, builderType);
        setterMethod.getModifierList().addAnnotation(NONNULL);

        setterMethod.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        setterMethod.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);

        final PsiParameter setterParameter = createSetterParameter(getterMethod, parameterType, methodName);
        setterMethod.getParameterList().add(setterParameter);

        final PsiCodeBlock body = setterMethod.getBody();
        assert body != null;
        body.delete();

        return setterMethod;
    }

    @NotNull
    private PsiMethod generateBuildMethod(@NotNull final PsiClass targetClass) {
        final PsiType targetClassType = psiElementFactory.createType(targetClass);
        final PsiMethod buildMethod = psiElementFactory.createMethod(BUILD_METHOD_NAME, targetClassType);

        buildMethod.getModifierList().addAnnotation(NONNULL);
        buildMethod.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        buildMethod.getModifierList().setModifierProperty(PsiModifier.ABSTRACT, true);

        final PsiCodeBlock body = buildMethod.getBody();
        assert body != null;
        body.delete();

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
        final PsiModifierList modifierList = builderClass.getModifierList();
        assert modifierList != null;
        modifierList.setModifierProperty(PsiModifier.PUBLIC, true);
        modifierList.setModifierProperty(PsiModifier.STATIC, true);
        modifierList.setModifierProperty(PsiModifier.ABSTRACT, true);
        modifierList.addAnnotation(AUTO_VALUE_BUILDER);
        return builderClass;
    }

    private void deleteUnusedMethods(@NotNull PsiClass builderClass) {
        final Set<String> methodNamesToStore = selectedMethods.stream()
                .map(methodMember -> ((PsiMethod) methodMember.getPsiElement()).getName())
                .collect(Collectors.toSet());
        for (PsiMethod method : builderClass.getMethods()) {
            if (!methodNamesToStore.contains(method.getName()) && !BUILD_METHOD_NAME.equals(method.getName())) {
                method.delete();
            }
        }
    }
}
