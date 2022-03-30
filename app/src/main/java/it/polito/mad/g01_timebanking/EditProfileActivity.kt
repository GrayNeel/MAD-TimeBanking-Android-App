package it.polito.mad.g01_timebanking

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class EditProfileActivity : AppCompatActivity() {

    lateinit var profilePicture:ImageButton
    var fullName: String = ""
    lateinit var ivFullName: EditText
    lateinit var ivNickname: EditText
    lateinit var ivEmail: EditText
    lateinit var ivLocation: EditText
    var profilePicturePath: String? = null

    val CAPTURE_IMAGE_REQUEST = 1
    val PICK_IMAGE_REQUEST = 2



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        ivFullName = findViewById(R.id.editTextFullName)
        ivNickname = findViewById(R.id.editTextNickname)
        ivEmail = findViewById(R.id.editTextEmail)
        ivLocation = findViewById(R.id.editTextLocation)
        profilePicture = findViewById(R.id.profilePictureButton)

        profilePicture.setOnClickListener { showPopup(profilePicture) }
        val i = intent
        ivFullName.setText(i.getStringExtra("it.polito.mad.g01_timebanking.fullName"))
        ivNickname.setText(i.getStringExtra("it.polito.mad.g01_timebanking.nickname"))
        ivEmail.setText(i.getStringExtra("it.polito.mad.g01_timebanking.email"))
        ivLocation.setText(i.getStringExtra("it.polito.mad.g01_timebanking.location"))

        //TODO: Take path of the profile picture from the intent
        profilePicturePath = i.getStringExtra("it.polito.mad.g01_timebanking.profilePicturePath")

        if (profilePicturePath is String) {
            val bitMapProfilePicture = BitmapFactory.decodeFile(profilePicturePath)
            profilePicture.setImageBitmap(bitMapProfilePicture)
        }

    }

    override fun onBackPressed() {
        //TODO: riempire il result con i valori di tutti i campi
        val i2 = Intent()
        //i2.putExtra("fullName", ivFullName.text.toString())
        i2.putExtra("nickname", ivNickname.text.toString())
        i2.putExtra("email", ivEmail.text.toString())
        i2.putExtra("location", ivLocation.text.toString())
        i2.putExtra("profilePicturePath",profilePicturePath)
        setResult(Activity.RESULT_OK,i2)
        val sharedPref = this.getSharedPreferences(getString(R.string.preference_file_key),Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            println("In edit")
            putString(getString(R.string.name), ivFullName.text.toString())
            apply()
        }
        //TODO: Salva in un file tutti i campi
        super.onBackPressed() //finish is inside the onBackPressed()
    }


    private fun dispatchTakePictureIntent() {

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create the File where the photo should go
        val photoFile: File? = try {
            createImageFile(this)
        } catch (ex: IOException) {
            // Error occurred while creating the File
            null
        }
        // Continue only if the File was successfully created
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "it.polito.mad.g01_timebanking.fileprovider",
                it
            )
            //FileProvider return a content in the form
            //content://it.polito.mad.g01_timebanking.fileprovider/my_images/JPEG_20220329_123453_7193664665067830656.jpg
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

            //Original problem with resolveActivity as in the documentation:
            //This appears to be due to the new restrictions on "package visibility" introduced in Android 11.
            //Basically, starting with API level 30, if you're targeting that version or higher, your app cannot see, or directly interact with, most external packages without explicitly requesting allowance, either through a blanket QUERY_ALL_PACKAGES permission, or by including an appropriate <queries> element in your manifest.
            //https://stackoverflow.com/questions/62535856/intent-resolveactivity-returns-null-in-api-30
            //https://cketti.de/2020/09/03/avoid-intent-resolveactivity/
            try {
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST)
            }   catch (e: ActivityNotFoundException) {
                Toast.makeText(this,"Error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //@Throws(IOException::class)
    private fun createImageFile(context: Context): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)  ///storage/sdcard0/Pictures
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            //eg. /storage/emulated/0/Android/data/it.polito.mad.g01_timebanking/files/Pictures/JPEG_20220329_123453_7193664665067830656.jpg
            profilePicturePath = absolutePath
        }
    }

    //Add the photo to a gallery
    //https://developer.android.com/training/camera/photobasics#TaskGallery
    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(profilePicturePath)
            println(f.absolutePath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICK_IMAGE_REQUEST -> { //For Image Gallery
                if (resultCode == RESULT_OK && data != null && data.data != null) {

                }
                return
            }
            CAPTURE_IMAGE_REQUEST -> if (resultCode == RESULT_OK) { //For CAMERA
                //You can use image PATH that you already created its file by the intent that launched the CAMERA (MediaStore.EXTRA_OUTPUT)

                // by this point we have the camera photo on disk
                val takenImage = BitmapFactory.decodeFile(profilePicturePath)
                profilePicture.setImageBitmap(takenImage)
                galleryAddPic()
                // RESIZE BITMAP, see section below
                //https://guides.codepath.com/android/Accessing-the-Camera-and-Stored-Media
            } else { // Result was a failure
                Toast.makeText(this, "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
            }
        }

    fun showPopup(v: View) {
        val popup = PopupMenu(this, v)
        //Set on click listener for the menu
        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item -> onMenuItemClick(item) })
        popup.inflate(R.menu.edit_profile_picture_menu)
        popup.show()

    }

    fun onMenuItemClick(item: MenuItem): Boolean {
        Toast.makeText(this, "Selected Item: " + item.title, Toast.LENGTH_SHORT).show()
        return when (item.itemId) {
            R.id.gallery ->                 // do your code
                true
            R.id.camera ->                 // do your code
            {
                dispatchTakePictureIntent()
                return true
            }
            else -> false
        }
    }
}

