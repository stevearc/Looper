+ NodeProxy {
  looper { ^Looper(server, clock) }

  record {|key, beats=4, quant=nil, countdown=5, layer=0, playback=false, callback|
    this.looper.record(key, beats, quant, this.bus, nil, countdown, layer, playback, callback);
  }

  bounce {|key, beats=4, quant=nil, countdown=5, layer=0|
    this.record(key, beats, quant, this.bus, nil, countdown, layer, true, {
      this.clear(0);
    });
  }
}

+ Environment {
  looper { ^Looper.default }
}

+ EnvironmentRedirect {
  looper { ^Looper.default }
}

+ ProxySpace {
  looper { ^Looper(server, clock) }
}

