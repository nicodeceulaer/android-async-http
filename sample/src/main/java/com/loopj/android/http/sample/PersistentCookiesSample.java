/*
    Android Asynchronous Http Client Sample
    Copyright (c) 2014 Marek Sebera <marek.sebera@gmail.com>
    http://loopj.com

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.loopj.android.http.sample;

import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.loopj.android.http.PersistentCookieStore;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.sample.util.SampleJSON;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.CookieManager;
import java.net.CookieHandler;
import java.net.CookiePolicy;

public class PersistentCookiesSample extends SampleParentActivity {
    private boolean enableCookies = true;
    private boolean cookies_were_enabled = true;
    private static final String LOG_TAG = "PersistentCookiesSample";

    private CookieStore cookieStore;

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, Menu.NONE, "Enable cookies").setCheckable(true);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItemEnableCookies = menu.findItem(0);
        if (menuItemEnableCookies != null)
            menuItemEnableCookies.setChecked(enableCookies);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.isCheckable()) {
            item.setChecked(!item.isChecked());
            if (item.getItemId() == 0) {
                enableCookies = item.isChecked();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Use the application's context so that memory leakage doesn't occur.
        cookieStore = new PersistentCookieStore(getApplicationContext());

        // Set the new cookie store.
        getAsyncHttpClient().setCookieStore(cookieStore);

        super.onCreate(savedInstanceState);
    }

    @Override
    public int getSampleTitle() {
        return R.string.title_persistent_cookies;
    }

    @Override
    public boolean isRequestBodyAllowed() {
        return false;
    }

    @Override
    public boolean isRequestHeadersAllowed() {
        return true;
    }

    @Override
    public String getDefaultURL() {
        // The base URL for testing cookies.
        String url = PROTOCOL + "httpbin.org/cookies";

        // If the cookie store is empty, suggest a cookie.
        if(cookieStore.getCookies().isEmpty()) {
            url += "/set?time=" + System.currentTimeMillis();
        }

        return url;
    }

    @Override
    public AsyncHttpClient getAsyncHttpClient() {
        AsyncHttpClient ahc = super.getAsyncHttpClient();
        HttpClient client = ahc.getHttpClient();
        if (client instanceof DefaultHttpClient) {
            if(enableCookies)
            {
                if(!cookies_were_enabled)
                {
                    Toast.makeText(this, String.format("Cookies disabled -> enabled"), Toast.LENGTH_SHORT).show();

                    cookieStore.clear();  // get rid of old cookies

                    CookieManager manager = new CookieManager(CookiePolicy.ACCEPT_ALL, cookieStore);
                    CookieHandler.setDefault(manager);
                }
            }
            else
            { 
                if(cookies_were_enabled)
                {
                    Toast.makeText(this, String.format("Cookies enabled -> disabled"), Toast.LENGTH_SHORT).show();

                    cookieStore.clear();  // get rid of old cookies

                    CookieManager manager = new CookieManager(CookiePolicy.ACCEPT_NONE, cookieStore);
                    CookieHandler.setDefault(manager);
                }
            }
            cookies_were_enabled = enableCookies;
        }
        return ahc;
    }

    @Override
    public ResponseHandlerInterface getResponseHandler() {
        return new BaseJsonHttpResponseHandler<SampleJSON>() {
            @Override
            public void onStart() {
                clearOutputs();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, SampleJSON response) {
                debugHeaders(LOG_TAG, headers);
                debugStatusCode(LOG_TAG, statusCode);
                if (response != null) {
                    debugResponse(LOG_TAG, rawJsonResponse);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, SampleJSON errorResponse) {
                debugHeaders(LOG_TAG, headers);
                debugStatusCode(LOG_TAG, statusCode);
                debugThrowable(LOG_TAG, throwable);
                if (errorResponse != null) {
                    debugResponse(LOG_TAG, rawJsonData);
                }
            }

            @Override
            protected SampleJSON parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                return new ObjectMapper().readValues(new JsonFactory().createParser(rawJsonData), SampleJSON.class).next();
            }
        };
    }

    @Override
    public RequestHandle executeSample(AsyncHttpClient client, String URL, Header[] headers, HttpEntity entity, ResponseHandlerInterface responseHandler) {
        client.setEnableRedirects(true);
        return client.get(this, URL, headers, null, responseHandler);
    }

}
