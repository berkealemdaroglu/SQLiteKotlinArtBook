package com.ersinberkealemdaroglu.kotlinartbook




import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ersinberkealemdaroglu.kotlinartbook.databinding.ActivityArtBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import androidx.appcompat.app.ActionBar
import java.lang.Exception


class ArtActivity : AppCompatActivity() {
    private lateinit var binding : ActivityArtBinding
    private lateinit var actionBar : ActionBar
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedBitMap : Bitmap? = null
    private lateinit var database : SQLiteDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        actionBar?.isShowing

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info.equals("new")){
            binding.artNameText.setText("")
            binding.artistNameText.setText("")
            binding.yearNameText.setText("")
            binding.button.visibility = View.VISIBLE
            binding.imageView.setImageResource(R.drawable.default_plus)
        }else{
            binding.button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)
            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))
            val artnameIndex = cursor.getColumnIndex("artname")
            val artistNameIndex = cursor.getColumnIndex("artistname")
            val yearIndex = cursor.getColumnIndex("year")
            val imageIndex = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                binding.artNameText.setText(cursor.getString(artnameIndex))
                binding.artNameText.isFocusable = false
                binding.artistNameText.isFocusable = false
                binding.yearNameText.isFocusable = false
                binding.artistNameText.setText(cursor.getString(artistNameIndex))
                binding.yearNameText.setText(cursor.getString(yearIndex))

                val byteArray = cursor.getBlob(imageIndex)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
                binding.imageView.isEnabled = false
            }
            cursor.close()
        }

    }

    fun saveButton(view : View){
        val artName = binding.artNameText.text.toString()
        val artistName = binding.artistNameText.text.toString()
        val year = binding.yearNameText.text.toString()

        if (selectedBitMap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitMap!!,300)
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                //val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")
                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            } catch (e:Exception){
                e.printStackTrace()
            }
            val intent = Intent(this@ArtActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)//Arkada ne kadar activity varsa kapatıp main activitye gidiyor!
            startActivity(intent)

        }
    }

    private fun makeSmallerBitmap(image : Bitmap, maximumSize : Int) : Bitmap{
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()
        if (bitmapRatio > 1){
            //landspace
            width = maximumSize
            val scaleHeight = width / bitmapRatio
            height = scaleHeight.toInt()
        }else{
            //portrait
            height = maximumSize
            val scaleWidth = width / bitmapRatio
            width = scaleWidth.toInt()
        }

        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view: View){
        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){ //PERMISSION_GRANTED - izin verildi

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                //rationale
                Snackbar.make(view,"Permission Needed For Gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                    //Request Permission
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()
            }else{
                //Request Permission
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }


        }else{

            val intentToGalary = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGalary)
        //intent
        }
    }

    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if (result.resultCode == RESULT_OK){
                val intentFromResult = result.data
                if (intentFromResult != null){
                    println("result ok")
                    val imageDataUrl = intentFromResult.data
                    if (imageDataUrl != null){
                        println("ok")
                    //binding.imageView.setImageURI(imageDataUrl)
                    try {
                        if (Build.VERSION.SDK_INT >= 28){
                            val source = ImageDecoder.createSource(this@ArtActivity.contentResolver, imageDataUrl)
                            selectedBitMap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(selectedBitMap)
                        }else{
                            selectedBitMap = MediaStore.Images.Media.getBitmap(contentResolver,imageDataUrl)
                            binding.imageView.setImageBitmap(selectedBitMap)
                        }

                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                    }
                }
            }

        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->
            if (result){
                //Permission Granted
                val intentToGalary = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGalary)
            }else{
                //Permission Denied
                Toast.makeText(this@ArtActivity,"Permission Needed!",Toast.LENGTH_LONG).show()
            }
        }

    }
}