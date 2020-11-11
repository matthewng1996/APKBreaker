package APKBreaker;

import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.OutputStream;

public class TextAreaOuputStream extends OutputStream
{
    private TextArea    output;

    public TextAreaOuputStream(TextArea ta) {
        this.output = ta;
    }

    @Override
    public void write(int i) throws IOException {
        output.appendText(String.valueOf((char) i));
    }
}


