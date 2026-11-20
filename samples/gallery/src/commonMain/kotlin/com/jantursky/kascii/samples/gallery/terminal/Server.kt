package com.jantursky.kascii.samples.gallery.terminal

import com.jantursky.kascii.components.AsciiTone
import kotlin.math.roundToInt
import kotlin.math.sin

/** How a server is doing. */
enum class Health(
    val label: String,
    val tone: AsciiTone,
) {
    Healthy("ok", AsciiTone.Success),
    Degraded("slow", AsciiTone.Warning),
    Down("down", AsciiTone.Error),
    ;

    val needsAttention: Boolean get() = this != Healthy
}

/** One server of the sample fleet. [load] is the recent history from 0 to 1, oldest first. */
data class Server(
    val id: String,
    val region: String,
    val health: Health,
    val seed: Int,
    val load: List<Float>,
    val log: List<String>,
) {
    val currentLoad: Float get() = load.last()

    /** Requests per minute, which follow the load. */
    val requests: Int get() = (currentLoad * REQUESTS_AT_FULL_LOAD).roundToInt()

    /** The change of the load over the last sample, in percent. */
    val loadChange: Float
        get() {
            val before = load.getOrNull(load.lastIndex - 1) ?: return 0f
            return if (before <= 0f) 0f else (load.last() / before - 1f) * 100f
        }

    /** The history with one more reading, which a server that is down does not have. */
    fun sampled(reading: Float): Server =
        copy(load = (load + if (health == Health.Down) 0f else reading).takeLast(HISTORY))

    fun restarted(): Server =
        copy(health = Health.Healthy, log = (log + "$ systemctl restart $id" + "$id started").takeLast(LOG_LINES))

    private companion object {
        const val REQUESTS_AT_FULL_LOAD = 1200
        const val LOG_LINES = 40
    }
}

/** How many of the latest readings a server that starts down has already missed. */
private const val DOWN_READINGS = 6

/** The number of load readings a server keeps. */
const val HISTORY = 24

/** The reading of server [seed] at [step]: a slow wave, so the sample data moves without a random source. */
fun readingAt(
    seed: Int,
    step: Int,
): Float = (0.5f + 0.35f * sin(step * 0.55f + seed)).coerceIn(0.02f, 0.98f)

/** The servers the app starts with. */
object Fleet {
    val servers: List<Server> =
        listOf(
            server("web-1", "eu-west", Health.Healthy, 1),
            server("web-2", "eu-west", Health.Degraded, 2),
            server("web-3", "us-east", Health.Healthy, 3),
            server("api-1", "us-east", Health.Healthy, 4),
            server("api-2", "ap-south", Health.Down, 5),
            server("db-1", "eu-west", Health.Healthy, 6),
            server("db-2", "us-east", Health.Degraded, 7),
            server("cache-1", "ap-south", Health.Healthy, 8),
            server("queue-1", "eu-west", Health.Healthy, 9),
            server("batch-1", "us-east", Health.Healthy, 10),
        )

    private fun server(
        id: String,
        region: String,
        health: Health,
        seed: Int,
    ) = Server(
        id,
        region,
        health,
        seed,
        load =
            List(HISTORY) {
                if (health == Health.Down &&
                    it >= HISTORY - DOWN_READINGS
                ) {
                    0f
                } else {
                    readingAt(seed, it)
                }
            },
        log =
            listOf(
                "$ systemctl status $id",
                "$id.service - $region",
                "   Active: ${if (health == Health.Down) "failed" else "active (running)"}",
                "$ tail -n 2 /var/log/app.log",
                if (health == Health.Healthy) "listening on :8080" else "upstream timeout after 30s",
            ),
    )
}
