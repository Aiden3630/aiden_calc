package com.aiden.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.PI

class CalculatorEngine {
    data class State(
        val expression: String = "",
        val display: String = "0",
        val error: Boolean = false,
    )

    fun append(state: State, token: String): State {
        val base = if (state.error) "" else state.expression
        val normalized = when (token) {
            "×" -> "*"
            "÷" -> "/"
            else -> token
        }
        val expression = base + normalized
        return state.copy(expression = expression, display = formatExpression(expression), error = false)
    }

    fun clear(state: State) = state.copy(expression = "", display = "0", error = false)

    fun backspace(state: State): State {
        if (state.error || state.expression.isEmpty()) return clear(state)
        val expression = state.expression.dropLast(1)
        return state.copy(expression = expression, display = formatExpression(expression), error = false)
    }

    fun formatExpression(expression: String) =
        expression.ifEmpty { "0" }
            .replace("*", "×")
            .replace("/", "÷")
            .replace("sqrt(", "√(")
            .replace("pi", "π")
            .replace("^2", "²")

    fun toggleSign(state: State): State {
        if (state.expression.isEmpty()) return state
        val match = Regex("""(-?\d+(?:\.\d+)?)$""").find(state.expression) ?: return state
        val value = match.value
        val replacement = if (value.startsWith("-")) value.drop(1) else "(-$value)"
        val expression = state.expression.replaceRange(match.range, replacement)
        return state.copy(expression = expression, display = formatExpression(expression))
    }

    fun evaluate(state: State): State {
        if (state.expression.isBlank()) return state
        val expression = normalizeForEvaluation(state.expression)
        return try {
            val result = format(Parser(expression).parse())
            state.copy(
                expression = result,
                display = result,
                error = false,
            )
        } catch (_: ArithmeticException) {
            state.copy(display = "Ошибка", error = true)
        } catch (_: IllegalArgumentException) {
            state.copy(display = "Ошибка", error = true)
        } catch (_: IllegalStateException) {
            state.copy(display = "Ошибка", error = true)
        }
    }

    private fun normalizeForEvaluation(expression: String): String {
        val normalized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("√", "sqrt")
            .replace("π", "pi")
        val balance = normalized.count { it == '(' } - normalized.count { it == ')' }
        return if (balance > 0) normalized + ")".repeat(balance) else normalized
    }

    private fun format(number: BigDecimal): String =
        number.stripTrailingZeros().toPlainString().let { if (it == "-0") "0" else it }

    private class Parser(private val input: String) {
        private var index = 0
        private val context = MathContext(16, RoundingMode.HALF_UP)

        fun parse(): BigDecimal {
            val value = expression()
            if (index != input.length) error("Unexpected token")
            return value
        }

        private fun expression(): BigDecimal {
            var value = term().value
            while (true) {
                value = when {
                    take('+') -> term().let { value.add(if (it.percentage) value.multiply(it.value, context) else it.value, context) }
                    take('-') -> term().let { value.subtract(if (it.percentage) value.multiply(it.value, context) else it.value, context) }
                    else -> return value
                }
            }
        }

        private fun term(): Parsed {
            var value = power()
            while (true) {
                value = when {
                    take('*') -> Parsed(value.value.multiply(power().value, context))
                    take('/') -> Parsed(value.value.divide(power().value, context))
                    else -> return value
                }
            }
        }

        private fun power(): Parsed {
            val base = unary()
            return if (take('^')) {
                Parsed(fromDouble(base.value.toDouble().pow(power().value.toDouble())))
            } else {
                base
            }
        }

        private fun unary(): Parsed = when {
            take('+') -> unary()
            take('-') -> unary().let { it.copy(value = it.value.negate()) }
            else -> postfix()
        }

        private fun postfix(): Parsed {
            var value = primary()
            while (true) {
                value = when {
                    take('%') -> Parsed(value.value.divide(BigDecimal("100"), context), percentage = true)
                    take('²') -> Parsed(value.value.multiply(value.value, context))
                    else -> return value
                }
            }
        }

        private fun primary(): Parsed {
            val name = identifier()
            if (name != null) {
                if (name == "pi" || name == "π") return Parsed(BigDecimal(PI, context))
                require(take('('))
                val argument = expression().also { require(take(')')) }
                return Parsed(function(name, argument))
            }
            return if (take('(')) {
                Parsed(expression().also { require(take(')')) })
            } else {
                Parsed(number())
            }
        }

        private fun function(name: String, argument: BigDecimal): BigDecimal {
            val value = argument.toDouble()
            val result = when (name) {
                "sin" -> sin(Math.toRadians(value))
                "cos" -> cos(Math.toRadians(value))
                "tan" -> tan(Math.toRadians(value))
                "sqrt" -> sqrt(value)
                "log" -> log10(value)
                "ln" -> ln(value)
                else -> error("Unknown function")
            }
            return fromDouble(result)
        }

        private fun fromDouble(value: Double): BigDecimal {
            require(value.isFinite())
            return BigDecimal.valueOf(value).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros()
        }

        private fun identifier(): String? {
            val start = index
            while (index < input.length && (input[index].isLetter() || input[index] == 'π')) index++
            if (start == index) return null
            return input.substring(start, index)
        }

        private fun number(): BigDecimal {
            val start = index
            while (index < input.length && (input[index].isDigit() || input[index] == '.')) index++
            require(start != index)
            return input.substring(start, index).toBigDecimal()
        }

        private fun take(char: Char): Boolean =
            if (index < input.length && input[index] == char) {
                index++
                true
            } else false

        private data class Parsed(val value: BigDecimal, val percentage: Boolean = false)
    }
}
