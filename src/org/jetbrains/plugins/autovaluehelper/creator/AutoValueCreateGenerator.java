package org.jetbrains.plugins.autovaluehelper.creator;

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.autovaluehelper.AutoValueBaseHelperGenerator;
import org.jetbrains.plugins.autovaluehelper.AutoValueUtils;

import java.util.List;

class AutoValueCreateGenerator extends AutoValueBaseHelperGenerator {

    private final Project project;
    private final PsiFile file;
    private final Editor editor;
    private final PsiElementFactory psiElementFactory;

    static void generate(@NotNull final Project project,
                         @NotNull final Editor editor,
                         @NotNull final PsiFile file,
                         @NotNull final List<PsiMethodMember> selectedMethods
    ) {
        final Runnable builderGenerator = new AutoValueCreateGenerator(project, file, editor, selectedMethods);
        ApplicationManager.getApplication().runWriteAction(builderGenerator);
    }

    private AutoValueCreateGenerator(
            @NotNull final Project project,
            @NotNull final PsiFile file,
            @NotNull final Editor editor,
            @NotNull final List<PsiMethodMember> selectedMethods
    ) {
        super(selectedMethods, project);
        this.project = project;
        this.file = file;
        this.editor = editor;
        this.psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    @Override
    public void run() {
        final PsiClass targetClass = AutoValueUtils.getStaticOrTopLevelClass(file, editor);
        if (targetClass == null) {
            return;
        }

        // generate create() method
        final PsiMethod newBuilderMethod = generateCreateMethod(targetClass);
        addMethod(targetClass, null, newBuilderMethod);

        JavaCodeStyleManager.getInstance(project).shortenClassReferences(file);
        CodeStyleManager.getInstance(project).reformat(targetClass);
    }

    @NotNull
    private PsiMethod generateCreateMethod(@NotNull PsiClass targetClass) {
        final PsiMethod createMethod = psiElementFactory
                .createMethod("create", psiElementFactory.createType(targetClass));
        createMethod.getModifierList().addAnnotation(NONNULL);
        createMethod.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
        createMethod.getModifierList().setModifierProperty(PsiModifier.STATIC, true);

        final StringBuilder parametersList = new StringBuilder();
        for (PsiMethodMember member : selectedMethods) {
            final PsiMethod getterMethod = member.getElement();
            final PsiType parameterType = getterMethod.getReturnType();
            final String parameterName = getterMethod.getName();
            assert parameterType != null;

            final PsiParameter setterParameter = createSetterParameter(getterMethod, parameterType, parameterName);
            createMethod.getParameterList().add(setterParameter);

            if (parametersList.length() > 0) {
                parametersList.append(",");
            }
            parametersList.append(parameterName);
        }

        final String className = getAutoValueClassName(targetClass);

        final PsiStatement returnStatement = psiElementFactory.createStatementFromText(
                String.format("return new %s(%s);", className, parametersList.toString()),
                createMethod
        );
        final PsiCodeBlock body = createMethod.getBody();
        assert body != null;
        body.add(returnStatement);

        return createMethod;
    }
}
