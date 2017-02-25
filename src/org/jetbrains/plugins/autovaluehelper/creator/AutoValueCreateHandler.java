package org.jetbrains.plugins.autovaluehelper.creator;

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.autovaluehelper.AutoValueBaseHelperHandler;

import java.util.List;

class AutoValueCreateHandler extends AutoValueBaseHelperHandler {

    @Override
    protected void generate(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull PsiFile file,
            @NotNull List<PsiMethodMember> existingFields
    ) {
        AutoValueCreateGenerator.generate(project, editor, file, existingFields);
    }
}
