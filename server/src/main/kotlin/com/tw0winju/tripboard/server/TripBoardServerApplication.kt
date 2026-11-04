package com.tw0winju.tripboard.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TripBoardServerApplication

fun main(args: Array<String>) {
    runApplication<TripBoardServerApplication>(*args)
}
