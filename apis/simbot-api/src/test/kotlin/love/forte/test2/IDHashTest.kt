package love.forte.test2

import love.forte.simbot.ID


fun main() {
    val map = mutableMapOf<ID, Int>()


    map[1.ID] = 1
    map["1".ID] = 2
    map[1.00.ID] = 3
    map[1.00F.ID] = 4

    println(map.size)
    println(map)


}