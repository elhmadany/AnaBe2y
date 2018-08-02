package com.tritrio.anabe2y

import android.arch.paging.PagedList
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.TextInputEditText
import android.support.v4.text.PrecomputedTextCompat
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.tritrio.anabe2y.models.Comment
import com.tritrio.anabe2y.utils.DateUtils.Companion.getTime
import kotlinx.android.synthetic.main.fragment_comments.*

class ShowCommentsFragment: BottomSheetDialogFragment() {
    companion object {
        fun newInstance(id: String): ShowCommentsFragment {
            val bundle = Bundle()
            bundle.putString("post_id", id)
            val fragment = ShowCommentsFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comments, container, false)
    }

    private val postId: String by lazy {
        arguments?.getString("post_id") ?: ""
    }
    private val db = FirebaseFirestore.getInstance().collection("items")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        refresh()
        list_comments.layoutManager = LinearLayoutManager(context)

        val user = FirebaseAuth.getInstance().currentUser

        comment_button.setOnClickListener {
            val builder = AlertDialog.Builder(context!!)
            val customView = LayoutInflater.from(context).inflate(R.layout.view_create_comment, null, false)
            val textInputEditText: TextInputEditText = customView.findViewById(R.id.create_comment_content)
            builder.setTitle("Enter a comment")
            builder.setPositiveButton("Post") { _, _ ->
                db.document(postId).collection("comments").add(Comment(
                        user?.displayName ?: "",
                        textInputEditText.text?.toString() ?: "",
                        System.currentTimeMillis(),
                        user?.uid ?: ""
                )).addOnSuccessListener {
                    refresh()
                }.addOnFailureListener {
                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                }
                db.document(postId).get().addOnSuccessListener {
                    db.document(postId).update("num_comments", it.getLong("num_comments")!! + 1)
                            .addOnSuccessListener {

                            }
                            .addOnFailureListener {

                            }
                }.addOnFailureListener {

                }
            }
            builder.setNegativeButton("Cancel") { _, _ -> }
            builder.setView(customView)
            builder.create().show()
        }
    }

    private fun refresh() {
        val query = db.document(postId).collection("comments").orderBy("timestamp", Query.Direction.DESCENDING)
        val config = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(10)
                .setPageSize(20)
                .build()
        val options = FirestorePagingOptions.Builder<Comment>()
                .setLifecycleOwner(this)
                .setQuery(query, config, Comment::class.java)
                .build()

        list_comments.adapter = CommentsAdapter(options, comments_progress, comments_none)
    }
}

class CommentsAdapter(options: FirestorePagingOptions<Comment>, private val progressView: View, private val noneView: View): FirestorePagingAdapter<Comment, CommentsAdapter.CommentViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): CommentViewHolder {
        return CommentViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.view_comment, parent, false))
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int, model: Comment) {
        holder.titleView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                holder.titleView.context.resources.getString(R.string.comment_title, model.author, getTime(model.timestamp)),
                TextViewCompat.getTextMetricsParams(holder.titleView), null))
        holder.contentView.setTextFuture(PrecomputedTextCompat.getTextFuture(
                model.content, TextViewCompat.getTextMetricsParams(holder.contentView), null))
    }

    override fun onLoadingStateChanged(state: LoadingState) {
        when(state) {
            LoadingState.LOADING_INITIAL -> {progressView.visibility = View.VISIBLE}
            LoadingState.LOADING_MORE -> {progressView.visibility = View.VISIBLE}
            LoadingState.LOADED -> {progressView.visibility = View.GONE; if(itemCount != 0) {noneView.visibility = View.GONE}}
            LoadingState.FINISHED -> {progressView.visibility = View.GONE; if(itemCount == 0) {noneView.visibility = View.VISIBLE}}
            LoadingState.ERROR -> {progressView.visibility = View.GONE}
        }
    }

    class CommentViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val contentView: AppCompatTextView = itemView.findViewById(R.id.comment_content)
        val titleView: AppCompatTextView = itemView.findViewById(R.id.comment_title)
    }
}