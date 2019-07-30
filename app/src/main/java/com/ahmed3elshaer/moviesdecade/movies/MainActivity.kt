package com.ahmed3elshaer.moviesdecade.movies

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.paging.PagedList
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ahmed3elshaer.moviesdecade.R
import com.ahmed3elshaer.moviesdecade.data.models.Movie
import com.ahmed3elshaer.moviesdecade.mvibase.MviView
import com.ahmed3elshaer.moviesdecade.utils.*
import com.google.android.material.snackbar.Snackbar
import dagger.android.AndroidInjection
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class MainActivity : AppCompatActivity(), MviView<MoviesIntents, MoviesViewStates>,
    HasActivityInjector {
    @Inject

    lateinit var activityDispatchingAndroidInjector: DispatchingAndroidInjector<Activity>

    override fun activityInjector(): DispatchingAndroidInjector<Activity> {
        return activityDispatchingAndroidInjector
    }

    private val disposables = CompositeDisposable()
    @Inject
    lateinit var viewModelFactory: MoviesViewModelFactory

    private val viewModel: MoviesViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders
            .of(this, viewModelFactory)
            .get(MoviesViewModel::class.java)
    }

    lateinit var moviesAdapter: MoviesAdapter
    lateinit var moviesSearchAdapter: MoviesSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        moviesAdapter = MoviesAdapter()
        moviesSearchAdapter = MoviesSearchAdapter()
        bind()
        initMoviesList()

    }

    private fun initMoviesList() {
        rvMovies.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvMovies.itemAnimator = DefaultItemAnimator()
        rvMovies.adapter = moviesAdapter
    }

    /**
     * Connect the [MviView] with the [MviViewModel]
     * We subscribe to the [MviViewModel] before passing it the [MviView]'s [MviIntent]s.
     * If we were to pass [MviIntent]s to the [MviViewModel] before listening to it,
     * emitted [MviViewState]s could be lost
     */
    private fun bind() {
        // Subscribe to the ViewModel and call render for every emitted state
        disposables.add(viewModel.states().subscribe(this::render))
        // Pass the UI's intents to the ViewModel
        viewModel.processIntents(intents())


    }

    override fun render(state: MoviesViewStates) {
        loadingState(state.isLoading)
        if (state.error != null) {
            showMessage(state.error.message)
            state.error.printStackTrace()
            return
        }
        state.movies?.let {
            when {
                state.movies.isEmpty() -> renderEmptyMovies()
                else -> renderMovies(state.movies, state.isSearch)
            }
        }


    }


    private fun renderMovies(movies: PagedList<Any>, isSearch: Boolean) {
        if (isSearch) {
            moviesSearchAdapter.submitList(movies)
            rvMovies.adapter = moviesSearchAdapter
        } else {
            moviesAdapter.submitList(movies)
            rvMovies.adapter = moviesAdapter
        }
        ivEmpty.hide()
        tvEmpty.hide()
        rvMovies.show()


    }

    private fun renderEmptyMovies() {
        ivEmpty.show()
        tvEmpty.show()
        rvMovies.hide()
    }

    private fun showMessage(message: String?) {
        message?.let {
            val view = findViewById<View>(android.R.id.content) ?: return
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .show()
        }

    }

    private fun loadingState(isLoading: Boolean) {
        if (isLoading)
            pbLoading.show()
        else
            pbLoading.invisible()


    }


    override fun intents(): Observable<MoviesIntents> {
        return Observable.merge(
            initIntent(),
            searchIntent()
        )
    }

    private fun initIntent(): Observable<MoviesIntents.InitIntent> {
        return Observable.just(MoviesIntents.InitIntent)
    }

    private fun searchIntent(): Observable<MoviesIntents.SearchIntent> {
        return RxSearchObservable.fromView(svMovies)
            .debounce(300, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                MoviesIntents.SearchIntent(it)
            }

    }


    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

}



