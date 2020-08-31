package org.opendedup.sdfs.mgmt.grpc;

import com.google.common.eventbus.Subscribe;

import org.opendedup.grpc.SDFSEventListRequest;
import org.opendedup.grpc.SDFSEventListResponse;
import org.opendedup.grpc.SDFSEventRequest;
import org.opendedup.grpc.SDFSEventResponse;
import org.opendedup.grpc.SDFSEventServiceGrpc;
import org.opendedup.grpc.errorCodes;
import org.opendedup.logging.SDFSLogger;

import io.grpc.stub.StreamObserver;

public class SDFSEventImpl extends SDFSEventServiceGrpc.SDFSEventServiceImplBase {

    @Override
    public void getEvent(SDFSEventRequest request, StreamObserver<SDFSEventResponse> responseObserver) {
        SDFSEventResponse.Builder b = SDFSEventResponse.newBuilder();
        try {
            b.setEvent(org.opendedup.sdfs.notification.SDFSEvent.getPotoBufEvent(request.getUuid()));
        } catch (NullPointerException e) {
            b.setError(e.getMessage());
            b.setErrorCode(errorCodes.ENOENT);
        } catch (Exception e) {
            SDFSLogger.getLog().error("unable to serialize message", e);
            b.setError("unable to serialize message");
            b.setErrorCode(errorCodes.EIO);
        }
        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
        return;
    }

    @Override
    public void subscribeEvent(SDFSEventRequest request, StreamObserver<SDFSEventResponse> responseObserver) {
        org.opendedup.sdfs.notification.SDFSEvent evt = null;
        try {
            evt = org.opendedup.sdfs.notification.SDFSEvent.getEvent(request.getUuid());
        } catch (NullPointerException e) {
            SDFSEventResponse.Builder b = SDFSEventResponse.newBuilder();
            b.setError(e.getMessage());
            b.setErrorCode(errorCodes.ENOENT);
            responseObserver.onNext(b.build());
            responseObserver.onCompleted();
        }
        SDFSEventListener l = new SDFSEventListener(evt, responseObserver);
        evt.registerListener(l);
        while (!evt.isDone()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                SDFSEventResponse.Builder b = SDFSEventResponse.newBuilder();
                b.setError(e.getMessage());
                b.setErrorCode(errorCodes.EIO);
                responseObserver.onNext(b.build());
                break;
            }
        }
        responseObserver.onCompleted();
    }

    public class SDFSEventListener {

        org.opendedup.sdfs.notification.SDFSEvent evt = null;
        StreamObserver<SDFSEventResponse> responseObserver;

        public SDFSEventListener(org.opendedup.sdfs.notification.SDFSEvent evt,
                StreamObserver<SDFSEventResponse> responseObserver) {
            this.evt = evt;
            this.responseObserver = responseObserver;
            evt.registerListener(this);
        }

        @Subscribe
        public void nvent(org.opendedup.sdfs.notification.SDFSEvent _evt) {
            SDFSEventResponse.Builder b = SDFSEventResponse.newBuilder();
            b.setEvent(_evt.toProtoBuf());
            responseObserver.onNext(b.build());
        }
    }

    @Override
    public void listEvents(SDFSEventListRequest request, StreamObserver<SDFSEventListResponse> responseObserver) {
        SDFSEventListResponse.Builder b = SDFSEventListResponse.newBuilder();
        try {
            b.addAllEvents(org.opendedup.sdfs.notification.SDFSEvent.getProtoBufEvents());
        } catch (Exception e) {
            SDFSLogger.getLog().error("unable to serialize message", e);
            b.setError("unable to serialize message");
            b.setErrorCode(errorCodes.EIO);
        }
        responseObserver.onNext(b.build());
        responseObserver.onCompleted();
        return;
    }

}