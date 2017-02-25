package org.jetbrains.plugins.autovaluehelper.builder;

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.autovaluehelper.AutoValueBaseHelperHandler;

import java.util.List;

class AutoValueBuilderHandler extends AutoValueBaseHelperHandler {

    @Override
    protected void generate(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull PsiFile file,
            @NotNull List<PsiMethodMember> existingFields
    ) {
        AutoValueBuilderGenerator.generate(project, editor, file, existingFields);
    }
}
