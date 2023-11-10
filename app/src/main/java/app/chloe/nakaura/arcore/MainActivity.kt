package app.chloe.nakaura.arcore

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.Toast
import androidx.annotation.RequiresApi
import app.chloe.nakaura.arcore.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.io.File

class MainActivity : AppCompatActivity() {
    lateinit var arFragment: ArFragment
    lateinit var viewRenderable: ViewRenderable
    companion object{
        var paintingToPost:Bitmap? = null
    }
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment

        var renderCount: Int = 0
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            renderCount++

            val arLayout =
                when(renderCount){
                1 -> R.layout.ar_layout_1
                2 -> R.layout.ar_layout_2
                3 -> R.layout.ar_layout_3
                else -> {
                    Toast.makeText(this,"You can only add up to 3 paint boards.",Toast.LENGTH_LONG).show()
                    return@setOnTapArPlaneListener
                }
            }

            ViewRenderable.builder()
                .setView(this, arLayout)
                .build()
                .thenAccept { renderable -> viewRenderable = renderable }
                .exceptionally {
                    it.printStackTrace()
                    Toast.makeText(this, "It cannot be read", Toast.LENGTH_LONG).show()
                    null
                }
            if (::viewRenderable.isInitialized) {
                //usable if it is initialized
                // Create the Anchor
                val anchor = hitResult.createAnchor()
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arFragment.arSceneView.scene)
                // Create the transformable andy and add it to the anchor
                val node = TransformableNode(arFragment.transformationSystem)
                node.setParent(anchorNode)
                node.renderable = viewRenderable
                node.select()
            }
        }

        //　push the photo button
        val pictureButton = findViewById<FloatingActionButton>(R.id.picture_button)
        pictureButton.setOnClickListener{
            paintingToPost = takeScreenShot()
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun takeScreenShot():Bitmap{
        val bitmap = Bitmap.createBitmap(
            arFragment.view?.width ?: 100,
            arFragment.view?.height ?: 100,
            Bitmap.Config.ARGB_8888
        )
        val intArray = IntArray(2)
        arFragment.view?.getLocationInWindow(intArray)
        try {
            PixelCopy.request(
                arFragment.arSceneView as SurfaceView,
                Rect(
                    intArray[0],
                    intArray[1],
                    intArray[0] + (arFragment.view?.width ?: 0),
                    intArray[1] + (arFragment.view?.height ?: 0)
                ),
                bitmap,
                { copyResult: Int ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        val mediaFolder = externalMediaDirs.first()
                        val file = File(
                            Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM),"${System.currentTimeMillis()}.jpeg")

                        //Bitmap saving
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, file.outputStream())
                        Toast.makeText(this, "The image was saved", Toast.LENGTH_SHORT).show()

                        val contentValues = ContentValues().apply {
                            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                            put("_data", file.absolutePath)
                        }

                        contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                    }
                },
                Handler()
            )
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            Toast.makeText(this@MainActivity, "The image was not saved。", Toast.LENGTH_LONG).show()
        }

        return bitmap
    }

}