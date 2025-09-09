package dev.mooner.eevee.view.info

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.mooner.eevee.databinding.ActivityInstallInfoBinding

class InstallInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstallInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityInstallInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnRequest.setOnClickListener {
            finish()
        }
        binding.ibBack.setOnClickListener {
            finish()
        }
        binding.btnCompanyFix.setOnClickListener {
            Toast.makeText(this@InstallInfoActivity, "정보가 반영되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}