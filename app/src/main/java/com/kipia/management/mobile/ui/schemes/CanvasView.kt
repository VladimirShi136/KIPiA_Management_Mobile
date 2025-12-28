package com.kipia.management.mobile.ui.schemes

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.*
import android.widget.Toast
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.DeviceLocation

class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Слушатели событий
    private var onDeviceDragListener: ((deviceId: Int, x: Float, y: Float) -> Unit)? = null
    private var onDeviceClickListener: ((deviceId: Int) -> Unit)? = null

    // Данные для отрисовки
    private var backgroundBitmap: Bitmap? = null
    private var deviceLocations: List<DeviceLocation> = emptyList()
    private var devices: List<Device> = emptyList()

    // Графические инструменты
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        textSize = 24f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f
        textAlign = Paint.Align.CENTER
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // ИСПРАВЛЕНО: Добавлен метод setOnDeviceDragListener
    fun setOnDeviceDragListener(listener: (deviceId: Int, x: Float, y: Float) -> Unit) {
        this.onDeviceDragListener = listener
    }

    // ИСПРАВЛЕНО: Добавлен метод setOnDeviceClickListener
    fun setOnDeviceClickListener(listener: (deviceId: Int) -> Unit) {
        this.onDeviceClickListener = listener
    }

    // ИСПРАВЛЕНО: Добавлен метод startDrag (заглушка для совместимости)
    fun startDrag(device: Device, view: View) {
        // В реальной реализации здесь должна быть логика начала перетаскивания
        // Сейчас просто показываем сообщение
        Toast.makeText(context, "Начато перетаскивание: ${device.inventoryNumber}", Toast.LENGTH_SHORT).show()

        // Можно эмулировать событие перетаскивания
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        // Сохраняем данные для возможной реализации
        // TODO: Реализовать настоящий механизм перетаскивания
    }

    // ИСПРАВЛЕНО: Добавлен метод setDeviceLocations
    fun setDeviceLocations(locations: List<DeviceLocation>) {
        this.deviceLocations = locations
        invalidate() // Перерисовать view
    }

    // ИСПРАВЛЕНО: Добавлен метод setBackgroundImage
    fun setBackgroundImage(bitmap: Bitmap) {
        this.backgroundBitmap = bitmap
        invalidate()
    }

    // ИСПРАВЛЕНО: Добавлен метод clearBackgroundImage
    fun clearBackgroundImage() {
        this.backgroundBitmap = null
        invalidate()
    }

    // ИСПРАВЛЕНО: Добавлен метод для установки списка устройств
    fun setDevices(devices: List<Device>) {
        this.devices = devices
        invalidate()
    }

    // ИСПРАВЛЕНО: Добавлен метод для получения устройства по ID
    fun getDeviceById(deviceId: Int): Device? {
        return devices.firstOrNull { it.id == deviceId }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Рисуем фон
        backgroundBitmap?.let { bitmap ->
            val rect = Rect(0, 0, width, height)
            canvas.drawBitmap(bitmap, null, rect, paint)
        } ?: run {
            // Белый фон если нет изображения
            canvas.drawColor(Color.WHITE)
        }

        // 2. Рисуем сетку (опционально)
        drawGrid(canvas)

        // 3. Рисуем устройства
        drawDevices(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Вертикальные линии
        for (x in 0..width step 50) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }

        // Горизонтальные линии
        for (y in 0..height step 50) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }
    }

    private fun drawDevices(canvas: Canvas) {
        for (location in deviceLocations) {
            val device = devices.firstOrNull { it.id == location.deviceId }
            device?.let {
                // Координаты с учетом смещения
                val centerX = location.x
                val centerY = location.y
                val radius = 30f

                // Рисуем круг (представление устройства)
                canvas.drawCircle(centerX, centerY, radius, paint)
                canvas.drawCircle(centerX, centerY, radius, borderPaint)

                // Рисуем текст с инвентарным номером
                val text = it.inventoryNumber.take(5) // Берем первые 5 символов
                canvas.drawText(text, centerX, centerY + 5, textPaint)

                // Рисуем поворотную метку
                drawRotationIndicator(canvas, centerX, centerY, radius, location.rotation)
            }
        }
    }

    private fun drawRotationIndicator(canvas: Canvas, x: Float, y: Float, radius: Float, rotation: Float) {
        val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
            style = Paint.Style.FILL
        }

        // Вычисляем позицию индикатора поворота
        val angle = Math.toRadians(rotation.toDouble())
        val indicatorX = x + (radius * 1.2f * kotlin.math.cos(angle.toFloat())).toFloat()
        val indicatorY = y + (radius * 1.2f * kotlin.math.sin(angle.toFloat())).toFloat()

        canvas.drawCircle(indicatorX, indicatorY, 5f, indicatorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Проверяем, попали ли в устройство
                val deviceId = findDeviceAtPoint(event.x, event.y)
                deviceId?.let {
                    onDeviceClickListener?.invoke(it)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Эмуляция перетаскивания
                val deviceId = findDeviceAtPoint(event.x, event.y)
                deviceId?.let {
                    onDeviceDragListener?.invoke(it, event.x, event.y)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findDeviceAtPoint(x: Float, y: Float): Int? {
        for (location in deviceLocations) {
            val distance = kotlin.math.sqrt(
                (x - location.x) * (x - location.x) +
                        (y - location.y) * (y - location.y)
            )
            if (distance <= 30f) { // Радиус клика
                return location.deviceId
            }
        }
        return null
    }
}