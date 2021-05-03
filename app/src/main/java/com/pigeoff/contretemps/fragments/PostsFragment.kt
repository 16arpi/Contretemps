package com.pigeoff.contretemps.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pigeoff.contretemps.CTApp
import com.pigeoff.contretemps.R
import com.pigeoff.contretemps.adapters.PostAdapter
import com.pigeoff.contretemps.client.HTTPClient
import com.pigeoff.contretemps.client.JSONPost
import com.pigeoff.contretemps.util.Util
import kotlinx.coroutines.*
import java.lang.Runnable
import kotlin.math.max

class PostsFragment(
    private var type: Int,
    private var num: Int,
    private var feature: Boolean,
    private var search: String?) : Fragment() {

    lateinit var recyclerView: RecyclerView
    lateinit var httpClient: HTTPClient
    lateinit var swiperefresh: SwipeRefreshLayout
    lateinit var adapter: PostAdapter

    var mDataLoaded: OnDatasLoaded? = null
    var isOkToLoad = true
    var loadingDeleted = false

    //Compteur d'articles
    var allPosts = arrayListOf<JSONPost>()
    var maxPages = 2
    var maxPosts = 2

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return View.inflate(context, R.layout.fragment_posts, null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        swiperefresh = view.findViewById(R.id.swiperefresh)
        recyclerView = view.findViewById(R.id.recyclerViewMain)

        //Init HTTP Client
        httpClient = (context?.applicationContext as CTApp).getHTTPClient()

        //Init RecyclerView (without nothing inside...)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = PostAdapter(requireContext(), arrayListOf(), feature)
        recyclerView.adapter = adapter


        //
        CoroutineScope(Dispatchers.IO).launch {
            fetch10Articles(search)
        }

        swiperefresh.setOnRefreshListener {
            CoroutineScope(Dispatchers.IO).launch {
                update(recyclerView, search)
            }
        }



    }

    override fun onResume() {
        super.onResume()

        httpClient.setOnPostsFetchedListener(object : HTTPClient.OnPostsFetchedListener {
            override fun onPostsFetchedListener(posts: ArrayList<JSONPost>?, maxPosts: Int?, maxPages: Int?) {
                addArticlesToRecycler(posts, maxPages, maxPosts)
            }
        })


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager?
                val adapter = recyclerView.adapter as PostAdapter

                if (!adapter.isLoading) {
                    if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() == adapter.itemCount - 1) { //bottom of list!
                        CoroutineScope(Dispatchers.Main).launch {
                            if (adapter.page <= maxPages) {
                                fetch10Articles(search)
                            }
                        }
                    }
                }
            }
        })
    }

    interface OnDatasLoaded {
        fun onOnDatasLoadedListener(load: Boolean)
    }

    fun setOnDataLoaded(listener: OnDatasLoaded) {
        this.mDataLoaded = listener
    }

    suspend fun fetch10Articles(search: String?) {
        adapter.isLoading = true

        withContext(Dispatchers.Main) {
            mDataLoaded?.onOnDatasLoadedListener(true)
        }

        if (isOkToLoad) {
            try {
                httpClient.posts(adapter.page, type, num, search)
            }
            catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    System.out.println(e)
                    Util.alertError(requireView(), true)
                    mDataLoaded?.onOnDatasLoadedListener(false)
                }
            }
        }
    }

    fun addArticlesToRecycler(posts: ArrayList<JSONPost>?, mPa: Int?, mPo: Int?) {
        //On arrête l'UI chargement

        maxPages = mPa ?: maxPages
        maxPosts = mPo ?: maxPosts

        System.out.println(adapter.page)
        System.out.println(maxPages)

        mDataLoaded?.onOnDatasLoadedListener(false)
        if (swiperefresh.isRefreshing) swiperefresh.isRefreshing = false
        adapter.isLoading = false

        //Si posts n'est pas null, on ajoute les nouveaux posts
        if (!posts.isNullOrEmpty()) {
            allPosts.addAll(posts)
            adapter.page++
            adapter.insertItems(posts)

            if (maxPosts > 0) {
                adapter.setMaxPosts(maxPosts)
            }
        }

        //Sinon on affiche l'erreur
        else {
            Util.alertError(requireView(), true)
        }

    }

    private suspend fun update(recyclerView: RecyclerView, search: String?) {
        allPosts = arrayListOf()
        maxPages = 2

        adapter = PostAdapter(requireContext(), arrayListOf(), feature)
        recyclerView.adapter = adapter

        fetch10Articles(search)

        withContext(Dispatchers.Main) {
            if (swiperefresh.isRefreshing) swiperefresh.isRefreshing = false
        }
    }

}