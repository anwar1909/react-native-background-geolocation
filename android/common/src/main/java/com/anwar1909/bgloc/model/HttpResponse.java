package com.anwar1909.bgloc.model;

import android.os.Parcel;
import android.os.Parcelable;

public class HttpResponse implements Parcelable {
    private final int statusCode;
    private final String body;

    public HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    protected HttpResponse(Parcel in) {
        statusCode = in.readInt();
        body = in.readString();
    }

    public static final Creator<HttpResponse> CREATOR = new Creator<HttpResponse>() {
        @Override
        public HttpResponse createFromParcel(Parcel in) {
            return new HttpResponse(in);
        }

        @Override
        public HttpResponse[] newArray(int size) {
            return new HttpResponse[size];
        }
    };

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(statusCode);
        parcel.writeString(body);
    }
}
