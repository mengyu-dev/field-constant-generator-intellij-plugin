import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by mengyu on 04/03/2017.
 */
public class GenerateFieldConstantForDB extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            Editor editor = getEditor(e.getDataContext(), project, false);
            actionPerformedImpl(project, editor);
        }
    }

    private void actionPerformedImpl(Project project, Editor editor) {
        if (editor == null) return;
        final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        if (psiFile == null) return;

        final GenerateFieldConstantForDBHandler handler = new GenerateFieldConstantForDBHandler(editor, project);

        CommandProcessor.getInstance().executeCommand(project, () -> {
            final Runnable action = () -> {
                if (!ApplicationManager.getApplication().isUnitTestMode() && !editor.getContentComponent().isShowing()) return;
                handler.invoke(project, editor, psiFile);
            };
            action.run();
        }, getCommandName(), DocCommandGroupId.noneGroupId(editor.getDocument()));

    }

    protected String getCommandName() {
        String text = getTemplatePresentation().getText();
        return text == null ? "" : text;
    }

    @Nullable
    protected Editor getEditor(@NotNull DataContext dataContext, @NotNull Project project, boolean forUpdate) {
        return CommonDataKeys.EDITOR.getData(dataContext);
    }

    @Override
    public void update(final AnActionEvent e) {
        //Get required data keys
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        //Set visibility only in case of existing project and editor
        e.getPresentation().setVisible((project != null && editor != null));

    }
}
