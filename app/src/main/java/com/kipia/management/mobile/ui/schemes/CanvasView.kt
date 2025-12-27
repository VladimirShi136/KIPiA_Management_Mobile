package com.kipia.management.mobile.ui.schemes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.DeviceLocation
import kotlin.math.pow
import kotlin.math.sqrt

class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Фоновое изображение схемы
    var backgroundBitmap: Bitmap? = null
        set(value) {
            field = value
            invalidate()
        }

    // Список приборов для отображения
    var devices: List<Device> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    // Расположения приборов на схеме
    var deviceLocations: List<DeviceLocation> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    // Текущий перетаскиваемый прибор
    private var draggedDevice: Device? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    // Настройки отрисовки
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.BLUE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    // Размер иконки прибора
    private val deviceIconSize = 60f

    // Callback для обновления позиции прибора
    var onDevicePositionChanged: ((deviceId: Int, x: Float, y: Float) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. Рисуем фон (если есть)
        backgroundBitmap?.let { bitmap ->
            // Масштабируем под размер View
            val scaleX = width.toFloat() / bitmap.width
            val scaleY = height.toFloat() / bitmap.height
            val scale = scaleX.coerceAtMost(scaleY)

            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale
            val left = (width - scaledWidth) / 2
            val top = (height - scaledHeight) / 2

            canvas.drawBitmap(bitmap, null, RectF(left, top, left + scaledWidth, top + scaledHeight), paint)
        }

        // 2. Рисуем сетку (если нет фона)
        if (backgroundBitmap == null) {
            drawGrid(canvas)
        }

        // 3. Рисуем приборы
        drawDevices(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val gridSize = 50f
        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        // Вертикальные линии
        for (x in 0..width step gridSize.toInt()) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), gridPaint)
        }

        // Горизонтальные линии
        for (y in 0..height step gridSize.toInt()) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), gridPaint)
        }
    }

    private fun drawDevices(canvas: Canvas) {
        devices.forEach { device ->
            val location = deviceLocations.firstOrNull { it.deviceId == device.id }
            val x = location?.x ?: 0f
            val y = location?.y ?: 0f

            // Пропускаем приборы без координат (кроме перетаскиваемого)
            if (x == 0f && y == 0f && device != draggedDevice) return@forEach

            // Определяем цвет в зависимости от типа прибора
            val deviceColor = getDeviceColor(device.type)
            paint.color = deviceColor

            // Рисуем круг (иконка прибора)
            val centerX = if (device == draggedDevice) dragOffsetX else x
            val centerY = if (device == draggedDevice) dragOffsetY else y

            canvas.drawCircle(centerX, centerY, deviceIconSize / 2, paint)

            // Обводка
            borderPaint.color = if (device == draggedDevice) Color.RED else Color.BLUE
            canvas.drawCircle(centerX, centerY, deviceIconSize / 2, borderPaint)

            // Текст с инвентарным номером (первые 5 символов)
            val shortNumber = if (device.inventoryNumber.length > 5) {
                device.inventoryNumber.take(5) + "..."
            } else {
                device.inventoryNumber
            }

            canvas.drawText(shortNumber, centerX, centerY + deviceIconSize, textPaint)
        }
    }

    private fun getDeviceColor(deviceType: String): Int {
        return when (deviceType.lowercase()) {
            "счетчик" -> Color.GREEN
            "датчик" -> Color.YELLOW
            "регулятор" -> Color.CYAN
            "клапан" -> Color.MAGENTA
            else -> Color.LTGRAY
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Проверяем, нажали ли на прибор
                val clickedDevice = findDeviceAtPoint(x, y)
                clickedDevice?.let { device ->
                    draggedDevice = device
                    dragOffsetX = x
                    dragOffsetY = y
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                draggedDevice?.let {
                    dragOffsetX = x
                    dragOffsetY = y
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggedDevice?.let { device ->
                    // Сохраняем новую позицию
                    onDevicePositionChanged?.invoke(device.id, x, y)
                    draggedDevice = null
                    invalidate()
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun findDeviceAtPoint(x: Float, y: Float): Device? {
        devices.forEach { device ->
            val location = deviceLocations.firstOrNull { it.deviceId == device.id }
            val deviceX = location?.x ?: 0f
            val deviceY = location?.y ?: 0f

            // Проверяем, попадает ли точка в круг прибора
            val distance = sqrt(
                (x - deviceX).toDouble().pow(2.0) +
                        (y - deviceY).toDouble().pow(2.0)
            )

            if (distance <= deviceIconSize / 2) {
                return device
            }
        }
        return null
    }

    // Метод для добавления нового прибора на схему
    fun addDeviceToCanvas(device: Device, x: Float, y: Float) {
        val newLocation = DeviceLocation(
            deviceId = device.id,
            schemeId = -1, // Временно, пока не знаем ID схемы
            x = x,
            y = y
        )

        // Добавляем во временный список
        deviceLocations = deviceLocations + newLocation
        onDevicePositionChanged?.invoke(device.id, x, y)
        invalidate()
    }
}