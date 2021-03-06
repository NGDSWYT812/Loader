package com.levylin.lib.net.loader;

import com.levylin.lib.net.INetworkView;
import com.levylin.lib.net.listener.OnLoadListener;
import com.levylin.lib.net.loader.helper.intf.ILoadStateHelper;
import com.levylin.lib.net.loader.helper.intf.IRefreshViewHelper;
import com.levylin.lib.net.loader.helper.listener.OnRefreshListener;
import com.levylin.lib.net.loader.helper.listener.OnReloadListener;
import com.levylin.lib.net.loader.listener.OnLoadFailureListener;
import com.levylin.lib.net.loader.listener.OnLoadSuccessListener;
import com.levylin.lib.net.loader.model.Model;

import io.reactivex.disposables.Disposable;

/**
 * 页面内容加载器
 * Created by LinXin on 2016/6/14 10:22.
 */
public class DataLoader<T> implements OnRefreshListener, OnReloadListener {

    private Model<T> model;
    ILoadStateHelper mLoadStateViewHelper;
    IRefreshViewHelper mRefreshViewHelper;
    OnLoadSuccessListener<T> onLoadSuccessListener;
    OnLoadFailureListener onLoadFailureListener;
    Disposable mSubscription;
    private INetworkView view;

    public DataLoader(INetworkView view, Model<T> model) {
        this.view = view;
        this.model = model;
        this.view.setDataLoader(this);
    }

    public void setLoadStateHelper(ILoadStateHelper helper) {
        this.mLoadStateViewHelper = helper;
        this.mLoadStateViewHelper.setReloadListener(this);
    }

    public void setRefreshViewHelper(IRefreshViewHelper helper) {
        this.mRefreshViewHelper = helper;
        mRefreshViewHelper.setOnRefreshListener(this);
    }

    public void setOnLoadSuccessListener(OnLoadSuccessListener<T> listener) {
        this.onLoadSuccessListener = listener;
    }

    public void setOnLoadFailureListener(OnLoadFailureListener listener) {
        this.onLoadFailureListener = listener;
    }

    public void load() {
        cancel();
        mSubscription = model.load(new OnLoadListener<T>() {
            @Override
            public void onStart() {
                showLoading();
            }

            @Override
            public void onSuccess(T response) {
                showContent(response);
                model.setManualRefresh(false);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                showError(throwable);
                model.setManualRefresh(false);
            }
        });
    }

    /**
     * 手动刷新
     */
    public void manualRefresh() {
        model.setManualRefresh(true);
        onRefresh();
    }

    @Override
    public void onRefresh() {
        model.preRefresh();
        load();
    }

    @Override
    public void onReLoad() {
        model.preReLoad();
        load();
    }

    /**
     * 取消加载
     */
    public void cancel() {
        if (mSubscription == null)
            return;
        if (mSubscription.isDisposed())
            return;
        mSubscription.dispose();
        mSubscription = null;
    }

    /**
     * 显示加载状态
     */
    private void showLoading() {
        if (isRefreshing())
            return;
        if (mLoadStateViewHelper == null)
            return;
        mLoadStateViewHelper.showLoading();
    }

    /**
     * 是否在刷新中
     *
     * @return true:是，false:否
     */
    protected boolean isRefreshing() {
        return (mRefreshViewHelper != null && mRefreshViewHelper.isRefreshing()) || model.isManualRefresh();
    }

    /**
     * 显示内容
     */
    private void showContent(T response) {
        if (isRefreshing()) {
            if (mRefreshViewHelper != null) {
                mRefreshViewHelper.refreshComplete(true);
            }
        }
        model.setData(isRefreshing(), response);
        if (mLoadStateViewHelper != null) {
            if (model.isEmpty()) {
                mLoadStateViewHelper.showEmpty();
            } else {
                mLoadStateViewHelper.showContent();
            }
        }
        if (onLoadSuccessListener != null) {
            onLoadSuccessListener.onSuccess(isRefreshing(), response);
        }
    }

    /**
     * 显示错误
     */
    private void showError(Throwable t) {
        if (isRefreshing()) {
            if (mRefreshViewHelper != null) {
                mRefreshViewHelper.refreshComplete(false);
            }
        } else if (mLoadStateViewHelper != null) {
            //界面为空
            mLoadStateViewHelper.showError(model.isEmpty(), t);
        }
        if (onLoadFailureListener != null) {
            onLoadFailureListener.onFailure(isRefreshing(), t);
        }
    }

    public void onDestroy() {
        cancel();
        view = null;
        mLoadStateViewHelper = null;
        mRefreshViewHelper = null;
    }
}
