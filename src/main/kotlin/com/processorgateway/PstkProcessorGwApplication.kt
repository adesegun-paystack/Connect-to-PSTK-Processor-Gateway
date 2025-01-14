package com.processorgateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PstkProcessorGwApplication

fun main(args: Array<String>) {
    runApplication<PstkProcessorGwApplication>(*args)
}
