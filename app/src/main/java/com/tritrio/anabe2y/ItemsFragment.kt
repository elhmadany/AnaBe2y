package com.tritrio.anabe2y

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import android.arch.paging.PagedList
import android.support.v4.text.PrecomputedTextCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.*
import android.widget.Toast
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.LoadingState
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_items.*
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.Log
import com.google.android.youtube.player.YouTubeStandalonePlayer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.tritrio.anabe2y.models.Item
import com.tritrio.anabe2y.utils.DateUtils
import kotlin.math.roundToInt


class MainFragment: Fragment() {
    private val db = FirebaseFirestore.getInstance().collection("items")
    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_items, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        items_refresh.isRefreshing = true

        items_refresh.setOnRefreshListener {
            refresh()
        }

        refresh()

        items_list.layoutManager = LinearLayoutManager(context)
    }

    fun refresh() {
        val query = db.orderBy("timestamp", Query.Direction.DESCENDING)
        val config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(10)
                .setPageSize(20)
                .build()
        val options = FirestorePagingOptions.Builder<Item>()
                .setLifecycleOwner(this)
                .setQuery(query, config, Item::class.java)
                .build()
        items_list.adapter = MyAdapter(options, activity!!, db,
                Resources.getSystem().displayMetrics.widthPixels - (Resources.getSystem().displayMetrics.density * 16),
                (Resources.getSystem().displayMetrics.density * 300).roundToInt())
    }
}

class MyAdapter(options: FirestorePagingOptions<Item>,
                private val activity: FragmentActivity,
                private val db: CollectionReference,
                private val postWidth: Float,
                private val defaultPostHeight: Int
): FirestorePagingAdapter<Item, MyAdapter.ItemViewHolder>(options) {
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): ItemViewHolder {
        return ItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_item, parent, false))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int, model: Item) {
        if(model.id == null) {
            model.id = getItem(position)?.id
        }
        holder.itemView.setOnClickListener {
            ShowCommentsFragment.newInstance(model.id ?: "").show(activity.supportFragmentManager, "fragment_comments")
        }
        if(!model.thumbnail.isNullOrEmpty()) {
            holder.thumbnailView.visibility = View.VISIBLE
            if(model.aspectRatio != null) {
                holder.thumbnailView.layoutParams.height =
                        (model.aspectRatio * postWidth).roundToInt()
                GlideApp.with(activity).load(model.thumbnail).transition(DrawableTransitionOptions.withCrossFade()).into(holder.thumbnailView)
            } else {
                holder.thumbnailView.layoutParams.height = defaultPostHeight
                GlideApp.with(activity).load(model.thumbnail).centerCrop().transition(DrawableTransitionOptions.withCrossFade()).into(holder.thumbnailView)
            }
        } else {
            holder.thumbnailView.visibility = View.GONE
        }

        if(model.youtube) {
            holder.youtubeView.visibility = View.VISIBLE
            holder.youtubeView.setOnClickListener {
                val youtubeIntent = YouTubeStandalonePlayer.createVideoIntent(
                        activity, "AIzaSyAB_EngUFRg4npct_2-OlE3QXx_J7ZMelY", model.link?.takeLast(11), 0, true, false)
                activity.startActivity(youtubeIntent)
            }
        } else {
            holder.youtubeView.visibility = View.GONE
        }

        if(model.likes == null) {
            db.document(model.id ?: "")
                    .collection("likes").document(user?.uid ?: "community")
                    .get().addOnSuccessListener {
                        if (it.exists()) {
                            if((it.get("likes") as Boolean)) {
                                DrawableCompat.setTint(holder.likeDrawable, ContextCompat.getColor(activity, R.color.material_green_500))
                                model.likes = true
                            } else {
                                model.likes = false
                                DrawableCompat.setTint(holder.likeDrawable, ContextCompat.getColor(activity, R.color.textColorSecondaryDefault))
                            }
                        } else {
                            model.likes = false
                            DrawableCompat.setTint(holder.likeDrawable, ContextCompat.getColor(activity, R.color.textColorSecondaryDefault))
                        }
                    }.addOnFailureListener {
                        model.likes = false
                        DrawableCompat.setTint(holder.likeDrawable, ContextCompat.getColor(activity, R.color.textColorSecondaryDefault))
                        Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show()
                    }
        }

        if(!model.description.isNullOrBlank()) {
            holder.separatorView.visibility = View.VISIBLE
            holder.descriptionView.visibility = View.VISIBLE
            holder.descriptionView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                    model.description!!, TextViewCompat.getTextMetricsParams(holder.descriptionView), null))
        } else {
            holder.separatorView.visibility = View.GONE
            holder.descriptionView.visibility = View.GONE
        }

        holder.titleView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                model.title ?: "", TextViewCompat.getTextMetricsParams(holder.titleView), null))
        holder.subtitleView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                holder.subtitleView.context.resources.getString(R.string.item_subtitle, model.author, DateUtils.getTime(model.timestamp)),
                TextViewCompat.getTextMetricsParams(holder.subtitleView), null))
        holder.scoreView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                model.score.toString(), TextViewCompat.getTextMetricsParams(holder.scoreView), null))
        holder.numCommentsView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                model.num_comments.toString(), TextViewCompat.getTextMetricsParams(holder.numCommentsView), null))
        holder.shareButton.setOnClickListener {
            if(!model.link.isNullOrEmpty()) {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, model.link)
                sendIntent.type = "text/plain"
                activity.startActivity(sendIntent)
            } else {
                Toast.makeText(activity, "Self posts sharing is coming soon", Toast.LENGTH_LONG).show()
            }
        }
        if(!model.link.isNullOrEmpty()) {
            holder.linkButton.visibility = View.VISIBLE
            holder.linkButton.setOnClickListener {
                CustomTabsIntent.Builder().build().launchUrl(holder.linkButton.context, Uri.parse(model.link))
            }
        } else {
            holder.linkButton.setOnClickListener(null)
            holder.linkButton.visibility = View.GONE
        }

        holder.likeButton.setOnClickListener {
            val data = HashMap<String, Any>(1)
            if(model.likes == true) {
                data["likes"] = false
                model.likes = false
                DrawableCompat.setTint(holder.likeDrawable, ContextCompat.getColor(activity, R.color.textColorSecondaryDefault))
                db.document(model.id ?: "")
                        .collection("likes").document(user?.uid ?: "community")
                        .set(data).addOnSuccessListener {
                            --model.score
                            db.document(model.id ?: "").update("score", model.score)
                                    .addOnSuccessListener {
                                        holder.scoreView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                                                model.score.toString(), TextViewCompat.getTextMetricsParams(holder.scoreView), null))
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show()
                                    }
                        }.addOnFailureListener {
                            DrawableCompat.setTint(holder.likeDrawable, ContextCompat.getColor(activity, R.color.material_green_500))
                            Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show()
                        }
            } else {
                data["likes"] = true
                model.likes = true
                DrawableCompat.setTint(holder.likeDrawable, ContextCompat.getColor(activity, R.color.material_green_500))
                db.document(model.id ?: "")
                        .collection("likes").document(user?.uid ?: "community")
                        .set(data).addOnSuccessListener {
                            ++model.score
                            db.document(model.id ?: "").update("score", model.score)
                                    .addOnSuccessListener {
                                        holder.scoreView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                                                model.score.toString(), TextViewCompat.getTextMetricsParams(holder.scoreView), null))
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show()
                                    }
                        }.addOnFailureListener {
                            DrawableCompat.setTint(holder.likeDrawable, ContextCompat.getColor(activity, R.color.textColorSecondaryDefault))
                            Toast.makeText(activity, it.message, Toast.LENGTH_LONG).show()
                        }
            }
        }
    }

    override fun onLoadingStateChanged(state: LoadingState) {
        when(state) {
            LoadingState.LOADING_INITIAL -> {activity.items_refresh.isRefreshing = true}
            LoadingState.LOADING_MORE -> {activity.items_refresh.isRefreshing = true}
            LoadingState.LOADED -> {activity.items_refresh.isRefreshing = false}
            LoadingState.FINISHED -> {activity.items_refresh.isRefreshing = false}
            LoadingState.ERROR -> {activity.items_refresh.isRefreshing = false;Toast.makeText(activity, "Something happened. please check your internet connection", Toast.LENGTH_LONG).show()}
        }
    }

    class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val thumbnailView: AppCompatImageView = itemView.findViewById(R.id.item_thumbnail)
        val titleView: AppCompatTextView = itemView.findViewById(R.id.item_title)
        val subtitleView: AppCompatTextView = itemView.findViewById(R.id.item_subtitle)
        val descriptionView: AppCompatTextView = itemView.findViewById(R.id.item_description)
        val separatorView: View = itemView.findViewById(R.id.separator0)
        val youtubeView: AppCompatImageButton = itemView.findViewById(R.id.item_youtube)
        val scoreView: AppCompatTextView = itemView.findViewById(R.id.item_score)
        val numCommentsView: AppCompatTextView = itemView.findViewById(R.id.item_comment_count)
        val shareButton: AppCompatImageButton = itemView.findViewById(R.id.item_share_button)
        val linkButton: AppCompatImageButton = itemView.findViewById(R.id.item_link_button)
        val likeButton: AppCompatImageButton = itemView.findViewById(R.id.item_like_button)
        val likeDrawable: Drawable = DrawableCompat.wrap(likeButton.drawable).mutate()
        init {
            val shareDrawable: Drawable = DrawableCompat.wrap(shareButton.drawable).mutate()
            DrawableCompat.setTint(shareDrawable, ContextCompat.getColor(shareButton.context, R.color.textColorSecondaryDefault))
            val linkDrawable: Drawable = DrawableCompat.wrap(linkButton.drawable).mutate()
            DrawableCompat.setTint(linkDrawable, ContextCompat.getColor(linkButton.context, R.color.textColorSecondaryDefault))
        }
    }
}