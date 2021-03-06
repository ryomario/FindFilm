package my.mobile.findfilm

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.BoringLayout.make
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import my.mobile.findfilm.adapter.TrailerAdapter
import my.mobile.findfilm.api.ApiConst
import my.mobile.findfilm.api.Repository
import my.mobile.findfilm.behavior.AppBarScrollWatcher
import my.mobile.findfilm.data.Film
import my.mobile.findfilm.data.Television
import my.mobile.findfilm.data.Trailer
import my.mobile.findfilm.databinding.ActivityDetailBinding
import my.mobile.findfilm.realm.RealmHelper
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DetailTvActivity : AppCompatActivity() {
    companion object {
        const val INTENT_EXTRA_NAME = "DetailTV"
    }


    private lateinit var binding: ActivityDetailBinding

    private lateinit var progressDialog: ProgressDialog
    private var menuItemFav: MenuItem? = null

    private lateinit var appBarScrollListener: AppBarScrollWatcher

    private lateinit var realm: RealmHelper

    private lateinit var tv: Television
    private val trailers: MutableList<Trailer> = mutableListOf()
    private lateinit var adapterTrailer: TrailerAdapter
    private var filmURL: String = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAppBar()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle(getString(R.string.tolong_tunggu))
        progressDialog.setCancelable(false)
        progressDialog.setMessage(getString(R.string.menampilkan_trailer))

        realm = RealmHelper(this)

        adapterTrailer = TrailerAdapter(this,trailers)
        binding.rvTrailer.adapter = adapterTrailer

        tv = intent.getSerializableExtra(INTENT_EXTRA_NAME) as Television

        tv.apply {
            filmURL = ApiConst.URLFILM + "" + this.id

            title = this.name
            binding.toolbar.title = this.name
            binding.name.text = this.originalName
            binding.rating.text = (this.vote_average.toString() + "/10")
            val date = this.realise_date.let {
                var hasil = "Unknown"
                val regex = """[0-2][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9]""".toRegex()
                if (regex.matches(it)){
                    val dateTime = LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val unix = dateTime.atStartOfDay(ZoneId.systemDefault())
                    hasil = unix.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                }
                hasil
            }
            binding.release.text = date
            binding.popularity.text = this.popularity.toString()
            binding.overview.text = this.overview
            binding.name.isSelected = true

            binding.ratingBar.numStars = 5
            binding.ratingBar.stepSize = 0.5f
            binding.ratingBar.rating = this.vote_average / 2

            Glide.with(this@DetailTvActivity)
                .load(ApiConst.URLIMAGE + this.backdrop_path)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgCover)
            Glide.with(this@DetailTvActivity)
                .load(ApiConst.URLIMAGE + this.backdrop_path)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(object : SimpleTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        binding.root.background = resource
                    }

                })
            Glide.with(this@DetailTvActivity)
                .load(ApiConst.URLIMAGE + this.poster_path)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imgPhoto)

            binding.rvTrailer.setHasFixedSize(true)
            binding.rvTrailer.layoutManager = LinearLayoutManager(this@DetailTvActivity)

            setFav(this@DetailTvActivity.realm.isTvFav(this))

            getTrailer()

        }


        binding.fav.setOnClickListener {
            favClick()
        }
        binding.share.setOnClickListener {
            shareClick()
        }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setFav(fav: Boolean){
        if (fav) {
            binding.fav.setImageResource(R.drawable.ic_favorit)
            menuItemFav?.icon = resources.getDrawable(R.drawable.ic_favorit)
            menuItemFav?.title = "Remove favorite"
        } else {
            binding.fav.setImageResource(R.drawable.ic_favorit_border)
            menuItemFav?.icon = resources.getDrawable(R.drawable.ic_favorit_border)
            menuItemFav?.title = "Add favorite"
        }
    }

    private fun favClick() {
        val isFav = realm.isTvFav(tv)
        if (isFav){
            realm.deleteTvFav(tv)
            Toast.makeText(applicationContext, "Removed to favorite", Toast.LENGTH_SHORT).show()
        } else {
            realm.addTvFav(tv)
            Toast.makeText(applicationContext, "Added to favorite", Toast.LENGTH_SHORT).show()
        }
        setFav(!isFav)
    }

    private fun shareClick() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        val subject = tv.originalName
        val desc = tv.overview
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, subject + "\n\n" + desc + "\n\n" + filmURL)
        startActivity(Intent.createChooser(intent, "Share with :"))
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getTrailer(){
        progressDialog.show()

        Repository.getTvTrailer(
            tv.id,
            onSuccess = {
                progressDialog.dismiss()
                trailers.clear()
                it.hasil.forEach{
                    trailers.add(it)
                    adapterTrailer.notifyDataSetChanged()
                }
            },
            onError = {
                Toast.makeText(this,"Failed to load trailers!",Toast.LENGTH_SHORT).show()
            }
        )
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menu is MenuBuilder){
            menu.setOptionalIconsVisible(true)
        }
        menuInflater.inflate(R.menu.menu_detail,menu)

        if (menu is Menu) menuItemFav = menu.findItem(R.id.action_favorit)!!

        return super.onCreateOptionsMenu(menu)
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        if (realm.isTvFav(tv)){
            menuItemFav?.icon = resources.getDrawable(R.drawable.ic_favorit)
            menuItemFav?.title = "Remove favorite"
        } else {
            menuItemFav?.icon = resources.getDrawable(R.drawable.ic_favorit_border)
            menuItemFav?.title = "Add favorite"
        }
        return super.onMenuOpened(featureId, menu)
    }

    // Fungsi untuk kembali ke activity sebelumnya
    override fun onBackPressed() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_share -> {
                shareClick()
                true
            }
            R.id.action_favorit -> {
                favClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Fungsi untuk mengatur background root view dan collapsed appbar
    private fun setupAppBar() {
        appBarScrollListener = AppBarScrollWatcher(object : AppBarScrollWatcher.OffsetListener {
            override fun onAppBarExpanding(
                expanded: Boolean,
                collapsed: Boolean,
                argbZeroOnExpanded: Int,
                argbZeroOnCollapsed: Int,
                alphaZeroOnCollapsed: Float,
                alphaZeroOnExpanded: Float
            ) {
                binding.cardViewPhoto.alpha = alphaZeroOnCollapsed
                binding.root.background?.alpha = argbZeroOnExpanded
                if (collapsed){
                    binding.cardViewPhoto.visibility = View.GONE
                } else {
                    binding.cardViewPhoto.visibility = View.VISIBLE
                }
            }

        })
        binding.appBar.addOnOffsetChangedListener(appBarScrollListener)
    }

    override fun onDestroy() {
        binding.appBar.removeOnOffsetChangedListener(appBarScrollListener)
        super.onDestroy()
    }
}