// Copyright 2012- Bill Campbell, Swami Iyer and Bahar Akbal-Delibas

package jminusminus;

import static jminusminus.CLConstants.*;

/**
 * The AST node for a long literal.
 */
class JLiteralFloat extends JExpression {
    // String representation of the literal.
    private String text;

    /**
     * Constructs an AST node for a long literal given its line number and string representation.
     *
     * @param line line in which the literal occurs in the source file.
     * @param text string representation of the literal.
     */
    public JLiteralFloat(int line, String text) {
        super(line);
        this.text = text;
    }

    /**
     * Returns the literal as a long.
     *
     * @return the literal as a long.
     */
    public long toFloat() {
        return Long.parseLong(text.substring(0, text.length() - 1));
    }

    /**
     * {@inheritDoc}
     */
    public JExpression analyze(Context context) {
        // TODO
        type = Type.FLOAT;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void codegen(CLEmitter output) {
        // TODO
        float f = toFloat();
        switch ((int) f) {
            case 0:
                output.addNoArgInstruction(FCONST_0);
                break;
            case 1:
                output.addNoArgInstruction(FCONST_1);
                break;
            case 2:
                output.addNoArgInstruction(FCONST_2);
                break;
            default:
                output.addLDCInstruction(f);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void toJSON(JSONElement json) {
        JSONElement e = new JSONElement();
        json.addChild("JLiteralLong:" + line, e);
        e.addAttribute("type", type == null ? "" : type.toString());
        e.addAttribute("value", text);
    }
}
