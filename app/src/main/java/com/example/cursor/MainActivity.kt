package com.example.cursor

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val serverIp = "10.42.40.147"
    private val serverPort = 5000

    private lateinit var socket: DatagramSocket

    private var lastX = 0f
    private var lastY = 0f

    private val commandQueue = ArrayDeque<String>()
    private val queueLock = Object()

    private var lastTapTime = 0L
    private var isDragging = false
    private var twoFingerTapStart = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val touchPad: View = findViewById(R.id.touchPad)
        val leftClick: TextView = findViewById(R.id.leftClick)
        val rightClick: TextView = findViewById(R.id.rightClick)

        socket = DatagramSocket()
        startSenderThread()

        touchPad.setOnTouchListener { _, event ->
            val pointerCount = event.pointerCount

            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y

                    if (pointerCount == 1) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 300) { // double-tap threshold
                            isDragging = true
                            sendCommand("CLICK_LEFT_DOWN")
                        }
                        lastTapTime = currentTime
                    } else if (pointerCount == 2) {
                        twoFingerTapStart = System.currentTimeMillis()
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val dxRaw = event.x - lastX
                    val dyRaw = event.y - lastY

                    val threshold = 1
                    val dx = if (Math.abs(dxRaw) > threshold) dxRaw.toInt() else 0
                    val dy = if (Math.abs(dyRaw) > threshold) dyRaw.toInt() else 0

                    if (dx != 0 || dy != 0) {
                        lastX = event.x
                        lastY = event.y

                        if (pointerCount == 1) {
                            sendCommand("MOVE $dx $dy")
                        } else if (pointerCount == 2) {
                            val scrollDy = dy.coerceIn(-30, 30) // smooth scroll
                            sendCommand("SCROLL $scrollDy")
                        }
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        sendCommand("CLICK_LEFT_UP")
                        isDragging = false
                    } else if (pointerCount == 1) {
                        sendCommand("CLICK_LEFT")
                    } else if (pointerCount == 2) {
                        val tapDuration = System.currentTimeMillis() - twoFingerTapStart
                        if (tapDuration < 200) sendCommand("CLICK_RIGHT")
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    if (pointerCount == 2) {
                        val tapDuration = System.currentTimeMillis() - twoFingerTapStart
                        if (!isDragging && tapDuration < 200) sendCommand("CLICK_RIGHT")
                    }
                }
            }
            true
        }

        leftClick.setOnClickListener { sendCommand("CLICK_LEFT") }
        rightClick.setOnClickListener { sendCommand("CLICK_RIGHT") }
    }

    private fun sendCommand(command: String) {
        synchronized(queueLock) {
            commandQueue.add(command)
        }
    }

    private fun startSenderThread() {
        thread {
            while (true) {
                var cmd: String? = null
                synchronized(queueLock) {
                    if (commandQueue.isNotEmpty()) {
                        cmd = commandQueue.removeFirst()
                    }
                }
                if (cmd != null) {
                    try {
                        val buf = cmd!!.toByteArray()
                        val packet = DatagramPacket(buf, buf.size, InetAddress.getByName(serverIp), serverPort)
                        socket.send(packet)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                Thread.sleep(2)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::socket.isInitialized) socket.close()
    }
}
