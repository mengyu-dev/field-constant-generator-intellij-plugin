import com.google.common.base.CaseFormat;
import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.codeInsight.generation.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mengyu on 05/03/2017.
 */
public class GenerateFieldConstantForDBHandler extends GenerateMembersHandlerBase{
    public static final String TITLE = "Database field constant generation";
    public static final String FIELDS = "FIELDS";

    private final Project project;
    private final Editor editor;

    public GenerateFieldConstantForDBHandler(Editor editor, Project project ) {
        super(TITLE);
        this.editor = editor;
        this.project = project;
    }

    @Override
    protected ClassMember[] getAllOriginalMembers(PsiClass aClass) {
        PsiField[] fields = aClass.getFields();
        ArrayList<ClassMember> array = new ArrayList<>();
        ImplicitUsageProvider[] implicitUsageProviders = Extensions.getExtensions(ImplicitUsageProvider.EP_NAME);
        PsiClass fieldsClass = aClass.findInnerClassByName(FIELDS, false);

        fieldLoop:
        for (PsiField field : fields) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue;

            if (fieldsClass != null){
                if(existsConstant(field.getNameIdentifier().getText(), fieldsClass)) continue ;
            }

            for (ImplicitUsageProvider provider : implicitUsageProviders) {
                if (provider.isImplicitWrite(field)) continue fieldLoop;
            }
            array.add(new PsiFieldMember(field));
        }
        return array.toArray(new ClassMember[array.size()]);
    }

    private static boolean existsConstant(String field, PsiClass fieldsClass){
        if(fieldsClass == null) return false;
        if(fieldsClass.findFieldByName(generateConstantFieldName(field), false) != null) return true;
        for(PsiField constantField : fieldsClass.getFields()){
            //todo: verify only String fields
            String constantValue = StringUtils.substring(constantField.getInitializer().getText(), 1, -1);
            if (constantValue.equals(field)) return true;
        }
        return false;
    }

    @Override
    protected GenerationInfo[] generateMemberPrototypes(PsiClass aClass, ClassMember originalMember) throws IncorrectOperationException {
        return null;
    }

    protected List<? extends GenerationInfo> generateMemberPrototypes(PsiClass aClass, ClassMember[] members) throws IncorrectOperationException {
        ArrayList<GenerationInfo> array = new ArrayList<>();
        PsiClass fieldsClass = aClass.findInnerClassByName(FIELDS, false);

        if(fieldsClass == null) {
            fieldsClass = generateClassPrototype(aClass);
            array.add(new PsiGenerationInfo(fieldsClass));
        }

        for (ClassMember member : members) {
            PsiField element = (PsiField) ((PsiElementClassMember) member).getElement();
            final PsiField field = generateConstantFieldPrototype(element);
            if(field != null) {
                fieldsClass.add(field);
            }
        }

        return array;
    }

    private static PsiClass generateClassPrototype(@NotNull PsiClass psiClass){
        PsiElementFactory factory = JavaPsiFacade.getInstance(psiClass.getProject()).getElementFactory();
        PsiClass fieldsClass = factory.createClass("FIELDS");
        PsiUtil.setModifierProperty(fieldsClass, PsiModifier.PUBLIC, true);
        PsiUtil.setModifierProperty(fieldsClass, PsiModifier.STATIC, true);

        return fieldsClass;
    }

    public static PsiField generateConstantFieldPrototype(@NotNull PsiField field) {
        PsiElementFactory factory = JavaPsiFacade.getInstance(field.getProject()).getElementFactory();
        Project project = field.getProject();
        String name = field.getNameIdentifier().getText();
        String constantFieldName = generateConstantFieldName(name);
        try {

            PsiField constantField = factory.createField(constantFieldName, PsiType.getTypeByName("java.lang.String", project, field.getResolveScope()));

            PsiUtil.setModifierProperty(constantField, PsiModifier.PUBLIC, true);
            PsiUtil.setModifierProperty(constantField, PsiModifier.STATIC, true);
            PsiUtil.setModifierProperty(constantField, PsiModifier.FINAL, true);

            PsiExpression value = factory.createExpressionFromText("\"" + name + "\"", field);
            constantField.setInitializer(value);
            constantField = (PsiField)CodeStyleManager.getInstance(project).reformat(constantField);
            return constantField;
        }
        catch (IncorrectOperationException e) {
            return null;
        }
    }

    private static String generateConstantFieldName(String fieldName){
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, fieldName);
    }
}
