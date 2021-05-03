package com.pigeoff.contretemps.adapters

import com.pigeoff.contretemps.CTApp
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.pigeoff.contretemps.activities.PostActivity
import com.pigeoff.contretemps.R
import com.pigeoff.contretemps.client.HTTPClient
import com.pigeoff.contretemps.client.JSONPost
import com.pigeoff.contretemps.util.Util
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.adapter_loading.view.*
import kotlinx.android.synthetic.main.adapter_post.view.*
import kotlinx.coroutines.*

class PostAdapter(private val context: Context,
                  private var posts: ArrayList<JSONPost>,
                  private var feature: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var client = (context.applicationContext as CTApp).getHTTPClient()
    var page = 1
    var mPo = 2
    var isLoading = false
    val loadedImg = HashMap<Int, String>()

    val VIEW_NORMAL = 0
    val VIEW_FEATURED = 1
    val VIEW_LOADING = 2
    val VIEW_BLANK = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_FEATURED -> {
                ViewHolderPost(LayoutInflater.from(context).inflate(R.layout.adapter_post_featured, parent, false))
            }
            VIEW_LOADING -> {
                ViewHolderLoading(LayoutInflater.from(context).inflate(R.layout.adapter_loading, parent, false))
            }
            VIEW_BLANK -> {
                ViewHolderBlank(LayoutInflater.from(context).inflate(R.layout.adapter_blank, parent, false))
            }
            else -> {
                ViewHolderPost(LayoutInflater.from(context).inflate(R.layout.adapter_post, parent, false))
            }
        }
    }

    override fun getItemCount(): Int {
        return posts.count()
    }

    override fun getItemViewType(position: Int): Int {
        val reste = (position) % 5

        if (feature) {
            if (reste == 0) {
                return VIEW_FEATURED
            }
            else {
                return VIEW_NORMAL
            }
        }
        else {
            return VIEW_NORMAL
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val viewType = getItemViewType(position)

        if (viewType == VIEW_NORMAL || viewType == VIEW_FEATURED) {
            holder as ViewHolderPost
            val post = posts.get(position)

            var title = ""
            if (Build.VERSION.SDK_INT >= 24) {
                title = Html.fromHtml(post.title.get("rendered"), Html.FROM_HTML_MODE_LEGACY).toString()
            }
            else {
                title = Html.fromHtml(post.title.get("rendered")).toString()
            }
            holder.title.text = title
            holder.meta.text = Util.wpDateToString(post.date)

            holder.imgCover.setImageDrawable(context.getDrawable(R.drawable.ic_cloud))
            updateImg(client, position, holder.imgCover, post)

            holder.cardItem.setOnClickListener {
                val id = post.id
                val intent = Intent(context, PostActivity::class.java)
                intent.putExtra(Util.ACTION_ID, id)
                context.startActivity(intent)
            }
        }

    }

    class ViewHolderPost(v: View) : RecyclerView.ViewHolder(v) {
        val cardItem = v.cardItem
        val title = v.txtTitle
        val meta = v.txtMeta
        val imgCover = v.imgCover
    }

    class ViewHolderLoading(v: View) : RecyclerView.ViewHolder(v) {
        val progress = v.progressBarLoading
    }

    class ViewHolderBlank(v: View) : RecyclerView.ViewHolder(v)

    fun insertItems(items: ArrayList<JSONPost>) {
        val lastOldElement = posts.count()
        Log.i("INFO", "Last element id $lastOldElement")
        for (i in items) {
            posts.add(i)
        }
        notifyItemRangeInserted(lastOldElement, items.count())
    }

    fun updateImg(client: HTTPClient, position: Int, view: ImageView, post: JSONPost) {
        CoroutineScope(Dispatchers.IO).launch {
            var url = ""
            if (loadedImg.get(position) == null) {
                try {
                    url = client.returnImgCoverUrl(post)!!
                }
                catch (e: Exception) {
                    Log.i("ERROR", e.message.toString())
                }
            }
            else {
                url = loadedImg.get(position)!!
            }

            if (!url.isNullOrEmpty()) {
                withContext(Dispatchers.Main){
                    try {
                        Picasso.get()
                            .load(url)
                            .into(view)
                    }
                    catch (e: Exception) {
                        Log.i("ERROR", e.message.toString())
                    }
                }
            }

        }
    }

    fun setMaxPosts(mo: Int) {
        mPo = mo
    }

    fun updatePosts(newPosts: ArrayList<JSONPost>) {
        posts = newPosts
        Log.i("INFO", "Last element id ${posts.count()}")
        notifyDataSetChanged()
        notifyItemRangeChanged(0, itemCount)
    }

    fun getPosts() : ArrayList<JSONPost> {
        return posts
    }

    fun notifyLoadingOver() {
        notifyItemRemoved(itemCount-1)
    }
}