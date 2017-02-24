package org.jetbrains.plugins.autovaluehelper;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.jetbrains.plugins.autovaluehelper.AutoValueCollectorUtils.collectMethods;

class AutoValueBuilderHandler implements LanguageCodeInsightActionHandler {

    @Override
    public boolean isValidFor(@NotNull Editor editor, @NotNull PsiFile file) {
        final PsiClass clazz = AutoValueUtils.getStaticOrTopLevelClass(file, editor);
        if (clazz == null) {
            return false;
        }
        return isValidClass(clazz);
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
        final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        final Document currentDocument = psiDocumentManager.getDocument(file);
        if (currentDocument == null) {
            return;
        }

        psiDocumentManager.commitDocument(currentDocument);

        if (!CodeInsightUtilBase.prepareEditorForWrite(editor)) {
            return;
        }

        if (!FileDocumentManager.getInstance().requestWriting(editor.getDocument(), project)) {
            return;
        }

        final List<PsiMethodMember> existingFields = collectMethods(file, editor);
        if (existingFields != null && !existingFields.isEmpty()) {
            AutoValueBuilderGenerator.generate(project, editor, file, existingFields);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    private boolean isValidClass(@NotNull PsiClass clazz) {
        final PsiModifierList psiModifierList = clazz.getModifierList();
        if (psiModifierList == null) {
            return false;
        }
        final PsiAnnotation[] annotations = psiModifierList.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if ("com.google.auto.value.AutoValue".equals(annotation.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
