package com.tritrio.anabe2y

import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.button.MaterialButton
import android.support.design.widget.BottomSheetDialogFragment
import android.support.v7.widget.AppCompatImageView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.firebase.ui.auth.ui.ProgressView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.tritrio.anabe2y.models.Item
import kotlinx.android.synthetic.main.fragment_create_post.*
import org.jsoup.Jsoup
import java.io.ByteArrayOutputStream

class CreatePostFragment: BottomSheetDialogFragment() {
    private val glide: GlideRequests by lazy {
        GlideApp.with(activity!!)
    }
    
    companion object {
        fun newInstance(url: String?): CreatePostFragment {
            if(url != null) {
                val bundle = Bundle()
                bundle.putString("url", url)
                val fragment = CreatePostFragment()
                fragment.arguments = bundle
                return fragment
            }
            return CreatePostFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_post, container, false)
    }

    var thumbnail: String? = null
    var aspectRatio: Float? = null
    var path: String? = null

    val linkTextWatcher: TextWatcher = object: TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if(!s.isNullOrEmpty()) {
                create_description_layout.visibility = View.GONE
                create_description_icon.visibility = View.GONE
                create_description.removeTextChangedListener(descriptionTextWatcher)
            } else {
                create_description_layout.visibility = View.VISIBLE
                create_description_icon.visibility = View.VISIBLE
                create_description.addTextChangedListener(descriptionTextWatcher)
                createButton.isEnabled = false
            }
        }

        override fun afterTextChanged(s: Editable?) {
            createButton.isEnabled = false
            thumbnail = null
            aspectRatio = null
            glide.clear(imageView)
            if(Patterns.WEB_URL.matcher(s?.toString()).matches()) {
                create_link_layout.error = null
                create_link_layout.isErrorEnabled = false
                progressView.visibility = View.VISIBLE

                if(s.toString().startsWith("https://youtube.com") || s.toString().startsWith("https://youtu.be") ||  s.toString().startsWith("https://m.youtube.com") || s.toString().startsWith("https://www.youtube.com") || s.toString().startsWith("https://www.youtu.be")) {
                    val youtubeId = s.toString().takeLast(11)
                    val link = "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"
                    glide.load(link).fitCenter().transition(DrawableTransitionOptions.withCrossFade()).listener(object: RequestListener<Drawable> {
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            thumbnail = link
                            aspectRatio = if(resource != null) {
                                resource.intrinsicHeight.toFloat() / resource.intrinsicWidth.toFloat()
                            } else {
                                null
                            }
                            progressView.visibility = View.GONE
                            createButton.isEnabled = true
                            return false
                        }

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            progressView.visibility = View.GONE
                            createButton.isEnabled = true
                            return true
                        }
                    }).into(imageView)
                } else {
                    glide.load(s.toString()).fitCenter().transition(DrawableTransitionOptions.withCrossFade()).listener(object: RequestListener<Drawable> {
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            thumbnail = s?.toString()
                            aspectRatio = if(resource != null) {
                                resource.intrinsicHeight.toFloat() / resource.intrinsicWidth.toFloat()
                            } else {
                                null
                            }
                            progressView.visibility = View.GONE
                            createButton.isEnabled = true
                            return false
                        }

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            LoadOGData(object: OGResponse {
                                override fun finished(url: String?) {
                                    if(url != null) {
                                        glide.load(url).fitCenter().transition(DrawableTransitionOptions.withCrossFade()).listener(object: RequestListener<Drawable> {
                                            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                                thumbnail = url.toString()
                                                aspectRatio = if(resource != null) {
                                                    resource.intrinsicHeight.toFloat() / resource.intrinsicWidth.toFloat()
                                                } else {
                                                    null
                                                }
                                                progressView.visibility = View.GONE
                                                createButton.isEnabled = true
                                                return false
                                            }

                                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                                progressView.visibility = View.GONE
                                                createButton.isEnabled = true
                                                return true
                                            }
                                        }).into(imageView)
                                    } else {
                                        progressView.visibility = View.GONE
                                        createButton.isEnabled = true
                                    }
                                }
                            }).execute(s.toString())
                            return true
                        }
                    }).into(imageView)
                }
            } else {
                create_link_layout.error = "Not a valid link"
                create_link_layout.isErrorEnabled = true
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
    }

    val descriptionTextWatcher: TextWatcher = object: TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if(!s.isNullOrEmpty()) {
                create_link_layout.visibility = View.GONE
                create_link_icon.visibility = View.GONE
                create_link.removeTextChangedListener(linkTextWatcher)
                createButton.isEnabled = true
                thumbnail = null
                aspectRatio = null
            } else {
                create_link_layout.visibility = View.VISIBLE
                create_link_icon.visibility = View.VISIBLE
                create_link.addTextChangedListener(linkTextWatcher)
                createButton.isEnabled = false
            }
        }
    }

    private lateinit var imageView: AppCompatImageView
    private lateinit var progressView: ProgressBar
    private lateinit var createButton: MaterialButton
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        imageView = view.findViewById(R.id.create_image)
        progressView = view.findViewById(R.id.create_progress)
        createButton = view.findViewById(R.id.create_button)

        createButton.setOnClickListener {
            progressView.visibility = View.VISIBLE
            if(path != null) {
                glide.load(path).listener(object: RequestListener<Drawable> {
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        val ref = FirebaseStorage.getInstance().reference.child("images/" + path?.hashCode().toString()+resource?.hashCode().toString())
                        val bitmap = (resource as BitmapDrawable).bitmap
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.WEBP, 100, baos)
                        val uploadTask = ref.putBytes(baos.toByteArray())
                        uploadTask.continueWithTask { task ->
                            if (!task.isSuccessful) {
                                Toast.makeText(context, "Upload failed", Toast.LENGTH_LONG).show()
                                dismiss()
                            }
                            ref.downloadUrl
                        }.addOnCompleteListener {
                            if(it.isSuccessful) {
                                FirebaseFirestore.getInstance().collection("items").add(Item(
                                        create_title.text?.toString(),
                                        null,
                                        null,
                                        0,
                                        0,
                                        FirebaseAuth.getInstance().currentUser?.uid ?: "community",
                                        FirebaseAuth.getInstance().currentUser?.displayName ?: "community",
                                        System.currentTimeMillis(),
                                        it.result.toString(),
                                        (bitmap.height.toFloat()/bitmap.width.toFloat()),
                                        false,
                                        ref.path
                                )).addOnSuccessListener {
                                    progressView.visibility = View.GONE
                                    (activity?.supportFragmentManager?.findFragmentById(R.id.container_main) as MainFragment).refresh()
                                    dismiss()
                                }.addOnFailureListener {
                                    progressView.visibility = View.GONE
                                    Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG).show()
                                    dismiss()
                                }
                            } else {
                                Toast.makeText(context, "Upload failed", Toast.LENGTH_LONG).show()
                                dismiss()
                            }
                        }
                        return false
                    }

                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }
                }).into(imageView)
            } else {
                FirebaseFirestore.getInstance().collection("items").add(Item(
                        create_title.text?.toString(),
                        create_link.text?.toString(),
                        create_description.text?.toString(),
                        0,
                        0,
                        FirebaseAuth.getInstance().currentUser?.uid ?: "community",
                        FirebaseAuth.getInstance().currentUser?.displayName ?: "community",
                        System.currentTimeMillis(),
                        thumbnail,
                        aspectRatio,
                        thumbnail?.startsWith("https://img.youtube.com") == true,
                        null
                )).addOnSuccessListener {
                    progressView.visibility = View.GONE
                    (activity?.supportFragmentManager?.findFragmentById(R.id.container_main) as MainFragment).refresh()
                    dismiss()
                }.addOnFailureListener {
                    progressView.visibility = View.GONE
                    Toast.makeText(context, "Something went wrong", Toast.LENGTH_LONG).show()
                    dismiss()
                }
            }
        }

        upload_button.setOnClickListener {
//            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
//            getIntent.type = "image/*"

            val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"

            startActivityForResult(pickIntent, 1)
        }

        create_description.addTextChangedListener(descriptionTextWatcher)

        create_link.addTextChangedListener(linkTextWatcher)
        if(arguments?.isEmpty == false) {
            create_link.setText(arguments?.getString("url", ""))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 1) {
            if(resultCode == RESULT_OK && data != null) {
                glide.load(data.data).fitCenter().transition(DrawableTransitionOptions.withCrossFade()).listener(object: RequestListener<Drawable> {
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        if(resource is BitmapDrawable) {
                            create_link.removeTextChangedListener(linkTextWatcher)
                            create_description.removeTextChangedListener(descriptionTextWatcher)
                            upload_button.isEnabled = false
                            createButton.isEnabled = true
                            create_description_layout.visibility = View.GONE
                            create_description_icon.visibility = View.GONE
                            create_link_layout.visibility = View.GONE
                            create_link_icon.visibility = View.GONE
                            create_link.text = null
                            create_description.text = null
                            path = data.data.toString()
                        } else {
                            Toast.makeText(context, "Invalid image", Toast.LENGTH_LONG).show()
                        }
                        return false
                    }

                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        Toast.makeText(context, "Invalid image", Toast.LENGTH_LONG).show()
                        return true
                    }
                }).into(imageView)
            } else {
                Toast.makeText(context, "Invalid image", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        glide.clear(imageView)
    }
}

class LoadOGData(private val delegate: OGResponse): AsyncTask<String, Unit, String?>() {
    override fun doInBackground(vararg p0: String): String? {
        return try {
            Jsoup.connect(p0[0]).userAgent("")
                    .get().select("meta[property=og:image]")?.attr("content")
        } catch (e: Exception) {null}
    }

    override fun onPostExecute(result: String?) {
        delegate.finished(result)
    }
}

interface OGResponse {
    fun finished(url: String?)
}