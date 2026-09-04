package com.aiden.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {
    private val engine = CalculatorEngine()

    @Test fun `respects operator precedence`() = assertResult("2+3*4", "14")
    @Test fun `supports parentheses`() = assertResult("(2+3)*4", "20")
    @Test fun `percent converts a number to decimal fraction`() = assertResult("10%", "0.1")
    @Test fun `addition percent uses left operand`() = assertResult("200+10%", "220")
    @Test fun `subtraction percent uses left operand`() = assertResult("200-10%", "180")
    @Test fun `multiplication percent uses decimal fraction`() = assertResult("200*10%", "20")
    @Test fun `division percent uses decimal fraction`() = assertResult("200/10%", "2000")
    @Test fun `supports unary minus`() = assertResult("-3+5", "2")
    @Test fun `supports trigonometry in degrees`() = assertResult("sin(30)+cos(60)", "1")
    @Test fun `auto closes function parentheses`() = assertResult("sin(30", "0.5")
    @Test fun `malformed function reports error instead of throwing`() {
        assertTrue(engine.evaluate(CalculatorEngine.State(expression = "sin(")).error)
    }
    @Test fun `supports square root`() = assertResult("sqrt(81)", "9")
    @Test fun `supports power operator`() = assertResult("2^3", "8")
    @Test fun `supports pi constant`() = assertResult("pi", "3.141592653589793")

    @Test fun `reports division by zero`() {
        val result = engine.evaluate(CalculatorEngine.State(expression = "4/0"))
        assertTrue(result.error)
        assertEquals("Ошибка", result.display)
    }

    @Test fun `reports malformed expression`() {
        assertTrue(engine.evaluate(CalculatorEngine.State(expression = "2+")).error)
    }

    @Test fun `backspace removes expression tokens and resets empty expression`() {
        assertEquals("12", engine.backspace(CalculatorEngine.State(expression = "123", display = "123")).display)
        assertEquals("0", engine.backspace(CalculatorEngine.State()).display)
    }

    @Test fun `backspace resets error`() {
        assertEquals(CalculatorEngine.State(), engine.backspace(CalculatorEngine.State(expression = "2+", display = "Ошибка", error = true)))
    }

    @Test fun `display formatter hides internal multiplication and division tokens`() {
        assertEquals("2×3÷4", engine.formatExpression("2*3/4"))
    }

    private fun assertResult(expression: String, expected: String) {
        assertEquals(expected, engine.evaluate(CalculatorEngine.State(expression = expression)).display)
    }
}
