package com.qinhuai.corelib.expression

import net.objecthunter.exp4j.ExpressionBuilder
import java.util.Random

object ExpressionEngine {
    private val random = Random()
    
    fun evaluate(expression: String, variables: Map<String, Double> = emptyMap()): Double {
        val builder = ExpressionBuilder(expression)
        variables.forEach { (key, value) ->
            builder.variable(key)
        }
        val expr = builder.build()
        variables.forEach { (key, value) ->
            expr.setVariable(key, value)
        }
        return expr.evaluate()
    }
    
    fun randomDouble(min: Double, max: Double): Double {
        return min + (max - min) * random.nextDouble()
    }
    
    fun randomInt(min: Int, max: Int): Int {
        return random.nextInt(max - min + 1) + min
    }
    
    fun randomGaussian(mean: Double, stdDev: Double): Double {
        return mean + stdDev * random.nextGaussian()
    }
    
    fun randomExponential(lambda: Double): Double {
        return -Math.log(1 - random.nextDouble()) / lambda
    }
}
