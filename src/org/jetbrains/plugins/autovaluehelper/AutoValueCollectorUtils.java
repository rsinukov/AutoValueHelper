package org.jetbrains.plugins.autovaluehelper;

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class AutoValueCollectorUtils {

    private AutoValueCollectorUtils() {
    }

    @Nullable
    static List<PsiMethodMember> collectMethods(@NotNull final PsiFile file, @NotNull final Editor editor) {
        final int offset = editor.getCaretModel().getOffset();
        final PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        }

        final PsiClass clazz = PsiTreeUtil.getParentOfType(element, PsiClass.class);

        final List<PsiMethodMember> allMethods = new ArrayList<>();

        PsiClass classToExtractMethodsFrom = clazz;
        while (classToExtractMethodsFrom != null) {
            allMethods.addAll(collectMethodsInClass(classToExtractMethodsFrom));

            classToExtractMethodsFrom = classToExtractMethodsFrom.getSuperClass();
        }

        return allMethods;
    }

    @NotNull
    private static List<PsiMethodMember> collectMethodsInClass(@NotNull final PsiClass clazz) {
        final List<PsiMethodMember> classMethodMembers = new ArrayList<>();

        for (final PsiMethod method : clazz.getMethods()) {
            if (!method.hasModifierProperty(PsiModifier.ABSTRACT)) {
                continue;
            }
            if (method.isConstructor()) {
                continue;
            }

            final PsiClass containingClass = method.getContainingClass();
            if (containingClass != null) {
                classMethodMembers.add(buildMethodMember(method, containingClass, clazz));
            }
        }

        return classMethodMembers;
    }

    private static PsiMethodMember buildMethodMember(
            @NotNull final PsiMethod method,
            @NotNull final PsiClass containingClass,
            @NotNull final PsiClass clazz
    ) {
        return new PsiMethodMember(method,
                TypeConversionUtil.getSuperClassSubstitutor(containingClass, clazz, PsiSubstitutor.EMPTY)
        );
    }
}
