package cz.fl0xi.ovladacledtest

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("scenes", MODE_PRIVATE) }
    private var espUrl = "http://4.3.2.1"
    private lateinit var addressInput: EditText
    private var red = 255
    private var green = 0
    private var blue = 128
    private var brightness = 120
    private var effect = 0
    private var status: TextView? = null
    private var preview: View? = null
    private lateinit var sceneSpinner: Spinner

    private val effectNames = arrayOf("Jedna barva", "Blikání", "Dýchání", "Stírání", "Náhodné stírání", "Náhodné barvy", "Sweep", "Dynamický", "Barevný cyklus", "Duha", "Scan")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(24))
            setBackgroundColor(Color.rgb(18, 19, 27))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(text("OVLADAČ LED PÁSKU", 24, Color.WHITE, true))
        content.addView(text("TEST  •  WLED / ESP32", 14, Color.LTGRAY, false))

        content.addView(text("Adresa WLED", 16, Color.WHITE, true).apply { setPadding(0, dp(12), 0, 0) })
        addressInput = EditText(this).apply {
            setSingleLine(true)
            hint = "http://4.3.2.1"
            setText(prefs.getString("wled_url", "http://4.3.2.1"))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }
        content.addView(addressInput)
        content.addView(button("Použít adresu WLED") {
            var value = addressInput.text.toString().trim().trimEnd('/')
            if (value.isNotEmpty() && !value.startsWith("http://") && !value.startsWith("https://")) value = "http://$value"
            if (value.isNotEmpty()) {
                espUrl = value
                prefs.edit().putString("wled_url", value).apply()
                status?.text = "Adresa uložena: $value"
            }
        })
        preview = View(this).apply {
            setBackgroundColor(Color.rgb(red, green, blue))
        }
        content.addView(preview, LinearLayout.LayoutParams(-1, dp(72)).apply {
            topMargin = dp(18); bottomMargin = dp(14)
        })

        content.addView(text("Barva", 18, Color.WHITE, true))
        addColorSlider(content, "Červená", 0) { red = it; updatePreview() }
        addColorSlider(content, "Zelená", 1) { green = it; updatePreview() }
        addColorSlider(content, "Modrá", 2) { blue = it; updatePreview() }

        content.addView(text("Jas", 18, Color.WHITE, true).apply {
            setPadding(0, dp(16), 0, 0)
        })
        val brightLabel = text("Jas: $brightness / 255", 14, Color.LTGRAY, false)
        content.addView(brightLabel)
        val brightSeek = SeekBar(this).apply {
            max = 255; progress = brightness
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    brightness = p.coerceAtLeast(1)
                    brightLabel.text = "Jas: $brightness / 255"
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) { sendCurrent() }
            })
        }
        content.addView(brightSeek)

        content.addView(text("Efekt", 18, Color.WHITE, true).apply {
            setPadding(0, dp(16), 0, 0)
        })
        val effectSpinner = Spinner(this)
        effectSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, effectNames)
        effectSpinner.setSelection(effect)
        effectSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                effect = position
                sendCurrent()
            }
        }
        content.addView(effectSpinner)

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        controls.addView(button("Zapnout") { requestState(true) }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { rightMargin = dp(6) })
        controls.addView(button("Vypnout") { requestState(false) }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { leftMargin = dp(6) })
        content.addView(controls)

        content.addView(text("Uložené scény", 18, Color.WHITE, true).apply {
            setPadding(0, dp(18), 0, dp(4))
        })
        sceneSpinner = Spinner(this)
        content.addView(sceneSpinner)
        refreshScenes()
        val sceneButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        sceneButtons.addView(button("Uložit scénu") { saveSceneDialog() }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(5) })
        sceneButtons.addView(button("Použít scénu") { applySelectedScene() }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(5) })
        content.addView(sceneButtons)

        status = text("Připoj mobil ke stejné Wi‑Fi jako WLED. Výchozí adresa WLED AP je http://4.3.2.1.", 13, Color.LTGRAY, false).apply {
            setPadding(0, dp(18), 0, 0)
        }
        content.addView(status)

        setContentView(root)
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

    private fun addColorSlider(parent: LinearLayout, label: String, channel: Int, onChange: (Int) -> Unit) {
        parent.addView(text(label, 14, Color.LTGRAY, false))
        val seek = SeekBar(this).apply {
            max = 255
            progress = when (channel) { 0 -> red; 1 -> green; else -> blue }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    onChange(p)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) { sendCurrent() }
            })
        }
        parent.addView(seek)
    }

    private fun updatePreview() {
        preview?.setBackgroundColor(Color.rgb(red, green, blue))
    }

    private fun sendCurrent() = requestState(true)

    private fun requestState(turnOn: Boolean) {
        status?.text = "Odesílám příkaz do WLED…"
        val body = JSONObject().apply {
            put("on", turnOn)
            put("bri", brightness.coerceIn(1, 255))
            put("seg", JSONArray().put(JSONObject().apply {
                put("col", JSONArray().put(JSONArray().put(red).put(green).put(blue)))
                put("fx", effect.coerceIn(0, 255))
            }))
        }.toString()
        executor.execute {
            try {
                val conn = (URL(espUrl.trimEnd('/') + "/json/state").openConnection() as HttpURLConnection)
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                conn.disconnect()
                runOnUiThread { status?.text = if (code in 200..299) "WLED odpovědělo: OK" else "WLED vrátilo chybu: $code" }
            } catch (e: Exception) {
                runOnUiThread { status?.text = "Připojení selhalo. Zkontroluj Wi‑Fi a adresu WLED." }
            }
        }
    }

    private fun sceneArray(): JSONArray =
        try { JSONArray(prefs.getString("list", "[]")) } catch (_: Exception) { JSONArray() }

    private fun refreshScenes() {
        val arr = sceneArray()
        val names = mutableListOf<String>()
        for (i in 0 until arr.length()) names.add(arr.optJSONObject(i)?.optString("name", "Scéna") ?: "Scéna")
        if (names.isEmpty()) names.add("Žádné uložené scény")
        sceneSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
    }

    private fun saveSceneDialog() {
        val input = EditText(this).apply { hint = "Název scény" }
        AlertDialog.Builder(this)
            .setTitle("Uložit scénu")
            .setView(input)
            .setPositiveButton("Uložit") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "Moje scéna" }
                val arr = sceneArray()
                arr.put(JSONObject().apply {
                    put("name", name); put("r", red); put("g", green); put("b", blue)
                    put("brightness", brightness); put("effect", effect)
                })
                prefs.edit().putString("list", arr.toString()).apply()
                refreshScenes()
                status?.text = "Scéna „$name“ je uložená v telefonu."
            }
            .setNegativeButton("Zrušit", null)
            .show()
    }

    private fun applySelectedScene() {
        val arr = sceneArray()
        val index = sceneSpinner.selectedItemPosition
        if (index !in 0 until arr.length()) {
            status?.text = "Nejdřív si ulož nějakou scénu."
            return
        }
        val obj = arr.optJSONObject(index) ?: return
        red = obj.optInt("r", 255); green = obj.optInt("g", 0); blue = obj.optInt("b", 128)
        brightness = obj.optInt("brightness", 120); effect = obj.optInt("effect", 0)
        updatePreview()
        sendCurrent()
        status?.text = "Scéna „${obj.optString("name")}“ odeslána."
        // Rebuild to synchronize slider positions.
        buildUi()
    }
}
