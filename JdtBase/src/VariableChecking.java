import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import  visitors.VariableCheckingVisitor;
import java.io.IOException;

public class VariableChecking {
    public String readFile(String fileName) {
        try {
            String fileContent = Utils.readFileContent(fileName);
            return fileContent;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static void main(String[] args) {
        String filePath  = "JdtBase/Test/Test.java";
        String fileContent = (new VariableChecking()).readFile(filePath);
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(fileContent.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new VariableCheckingVisitor() );
    }
}
