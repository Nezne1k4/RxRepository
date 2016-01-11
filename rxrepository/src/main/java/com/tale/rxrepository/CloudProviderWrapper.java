/**
 * RxRepository
 *
 * Created by Giang Nguyen on 1/7/16.
 * Copyright (c) 2016 Umbala. All rights reserved.
 */

package com.tale.rxrepository;

import rx.Observable;
import rx.functions.Func0;

class CloudProviderWrapper<T> implements CloudProvider<T> {
  private CloudProvider<T> cloudProvider;
  private NetworkVerifier networkVerifier;

  public CloudProviderWrapper(CloudProvider<T> cloudProvider, NetworkVerifier networkVerifier) {
    this.cloudProvider = cloudProvider;
    this.networkVerifier = networkVerifier;
  }

  @Override public Observable<T> get(int page) {
    if (networkVerifier.isConnected()) {
      return cloudProvider.get(page);
    } else {
      return Observable.defer(new Func0<Observable<T>>() {
        @Override public Observable<T> call() {
          return Observable.error(new NoNetworkException());
        }
      });
    }
  }

  public CloudProvider<T> getCloudProvider() {
    return cloudProvider;
  }
}
