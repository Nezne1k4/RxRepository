/**
 * RxRepository
 *
 * Created by Giang Nguyen on 12/25/15.
 * Copyright (c) 2015 Umbala. All rights reserved.
 */

package com.tale.rxrepositorymosby;

import android.util.Log;
import com.hannesdorfmann.mosby.mvp.MvpBasePresenter;
import com.tale.rxrepository.ListRepository;
import java.util.List;
import java.util.NoSuchElementException;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RxRepositoryMvpLcePresenter<M, V extends MvpLcemView<List<M>>>
    extends MvpBasePresenter<V> {

  protected ListRepository<M> repository;
  protected Subscriber<List<M>> subscriber;
  protected boolean loadMore;
  protected boolean pullToRefresh;
  protected Throwable error;

  public RxRepositoryMvpLcePresenter(ListRepository<M> repository) {
    this.repository = repository;
  }

  /**
   * Unsubscribes the subscriber and set it to null
   */
  protected void unsubscribe() {
    if (subscriber != null && !subscriber.isUnsubscribed()) {
      subscriber.unsubscribe();
    }

    subscriber = null;
  }

  public void loadData() {
    subscribe(repository.get(), false);
  }

  public void refresh() {
    subscribe(repository.refresh(), true);
  }

  public void loadMore() {
    if (loadMore) {
      // No load more while it's loading.
      return;
    }
    loadMore = true;
    subscribe(repository.more(), false);
  }

  /**
   * Subscribes the presenter himself as subscriber on the observable
   *
   * @param observable The observable to subscribe
   * @param pullToRefresh Pull to refresh?
   */
  public void subscribe(Observable<List<M>> observable, final boolean pullToRefresh) {

    this.pullToRefresh = pullToRefresh;
    if (!loadMore && isViewAttached()) {
      getView().showLoading(pullToRefresh);
    }

    unsubscribe();

    subscriber = new Subscriber<List<M>>() {
      private boolean ptr = pullToRefresh;

      @Override public void onCompleted() {
        RxRepositoryMvpLcePresenter.this.onCompleted();
      }

      @Override public void onError(Throwable e) {
        RxRepositoryMvpLcePresenter.this.onError(e, ptr);
      }

      @Override public void onNext(List<M> m) {
        RxRepositoryMvpLcePresenter.this.onNext(m);
      }
    };

    observable.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(subscriber);
  }

  protected void onCompleted() {
    if (loadMore) {
      // If loadMore flash is not reset, that mean onNext will not be called or no data then we
      // reset loadMore flag and set notify the view that no more to hide load more progress.
      loadMore = false;
      if (isViewAttached()) {
        getView().onNoMore();
      }
    }
    if (error == null && !repository.hasCache()) {
      if (isViewAttached()) {
        // onCompleted is called but there is no error and no data then notify the view that no
        // element to show and it should show empty view.
        getView().showError(new NoSuchElementException(), this.pullToRefresh);
      }
    } else if (pullToRefresh) {
      if (isViewAttached()) {
        getView().showContent();
      }
    }
    unsubscribe();
    this.pullToRefresh = false;
  }

  protected void onError(Throwable e, boolean pullToRefresh) {
    this.error = e;
    if (isViewAttached()) {
      // Show light error in case pullToRefresh or has data displaying.
      getView().showError(e, pullToRefresh || repository.hasCache());
    }
    unsubscribe();
  }

  protected void onNext(List<M> data) {
    if (data != null && data.size() > 0) {
      for (int i = 0; i < data.size(); i++) {
        Log.d("Presenter", "item: " + data.get(i));
      }
      // If have data then we reset loadMore flag.
      loadMore = false;

      if (isViewAttached()) {
        final V view = getView();
        view.setData(data);
        view.showContent();
      }
    } else {
      // If no data then we show empty
      if (!repository.hasCache() && isViewAttached()) {
        getView().showError(new NoSuchElementException(), false);
      }
    }
  }

  @Override public void detachView(boolean retainInstance) {
    super.detachView(retainInstance);
    if (!retainInstance) {
      unsubscribe();
    }
  }
}