package edu.washington.cs.oneswarm.f2f.servicesharing;

public interface ServiceConnectionDelegate {
    void connected(ServiceConnection conn);

    void closing(ServiceConnection conn);
}
