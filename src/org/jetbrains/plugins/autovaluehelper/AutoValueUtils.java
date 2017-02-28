package org.jetbrains.plugins.autovaluehelper;

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AutoValueUtils {

    @Nullable
    public static PsiClass getStaticOrTopLevelClass(@NotNull PsiFile file, @NotNull Editor editor) {
        final int offset = editor.getCaretModel().getOffset();
        final PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return null;
        }

        PsiClass topLevelClass = PsiUtil.getTopLevelClass(element);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
        if (psiClass != null && (psiClass.hasModifierProperty(PsiModifier.STATIC) ||
                psiClass.getManager().areElementsEquivalent(psiClass, topLevelClass)))
            return psiClass;
        else
            return null;
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

        final Queue<PsiClass> classesToExtractMethodsFrom = new LinkedList<>();
        classesToExtractMethodsFrom.add(clazz);
        while (!classesToExtractMethodsFrom.isEmpty()) {
            final PsiClass currentClass = classesToExtractMethodsFrom.poll();
            allMethods.addAll(0, collectMethodsInClass(currentClass));

            final PsiClass[] interfaces = currentClass.getInterfaces();
            for (PsiClass interfaze : interfaces) {
                classesToExtractMethodsFrom.add(interfaze);
            }

            final PsiClass superClass = currentClass.getSuperClass();
            if (superClass != null) {
                classesToExtractMethodsFrom.add(superClass);
            }
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
