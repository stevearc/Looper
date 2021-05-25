Looper {
  classvar <>default, looperMap;
  var <server, <clock, <>buffers, <>synths;
  *initClass {
    looperMap = ();
    default = this.new(Server.default, TempoClock.default);
  }
  *new { |server, clock=nil|
    var serverMap, cached;
    clock = clock ? TempoClock.default;
    serverMap = looperMap[server] ? ();
    cached = serverMap[clock];
    if (cached.isNil) {
      cached = super.new.init(server, clock);
      looperMap[server] = serverMap;
      serverMap[clock] = cached;
    };
    ^cached;
  }
  *current { ^currentEnvironment.looper }
  init { |inServer, inClock|
    server = inServer;
    clock = inClock;
    buffers = IdentityDictionary.new;
    synths = IdentityDictionary.new;
    this.loadSynthDefs;
  }

  *buffers { ^Looper.current.buffers }

  *play {|key, loop=1| ^Looper.current.play(key, loop)}
  play {|key, loop=1|
    var buffer = buffers[key];
    if (buffer.isNil) {
      ("Buffer not found: " ++ key).throw;
    };
    if (synths[key].isNil) {
      synths[key] = Synth("loopPlay" ++ buffer.numChannels, [\buf, buffer, \loop, loop]);
      synths[key].onFree {
        synths[key] = nil;
      };
    };
    ^synths[key];
  }

  *stop {|key, immediate=false| Looper.current.stop(key, immediate)}
  stop {|key, immediate=false|
    if (key.isNil) {
      ^synths.keys.do { |synthKey|
        this.stop(synthKey, immediate);
      };
    };
    if (synths[key].notNil) {
      if (immediate) {
        synths[key].free;
      } {
        synths[key].set(\loop, 0);
      };
      synths[key] = nil;
    }
  }

  *record {|key, beats=4, quant=nil, bus=0, numChannels=nil, countdown=5, layer=0, playback=false, callback=nil|
    Looper.current.record(key, beats, quant, bus, numChannels, countdown, layer, playback, callback)}
  record {|key, beats=4, quant=nil, bus=0, numChannels=nil, countdown=5, layer=0, playback=false, callback=nil|
    var buf, synth;
    var time = beats / clock.tempo;
    var numFrames = server.sampleRate * time;
    var recordBeats = max(layer, 1) * beats;
    var countdownTime;
    if (quant.isNil) {
      quant = beats;
    };
    if (numChannels.isNil) {
      if (bus.class == Bus) {
        numChannels = bus.numChannels;
      } {
        numChannels = 2;
      };
    };
    buf = buffers[key];
    if (buf.notNil) {
      if (buf.numChannels != numChannels or: {buf.numFrames != numFrames}) {
        if (layer == 0) {
          this.free(key);
          buf = nil;
        } {
          var requiredBeats = buf.numFrames * clock.tempo / server.sampleRate;
          ("Buffer size or channel mismatch: cannot layer on recording "
          ++ key ++ ". Try recording for " ++ requiredBeats
          ++ " beats on " ++ buf.numChannels ++ " channels").throw;
        };
      };
    };
    if (buf.isNil) {
      buf = Buffer.alloc(server, numFrames, numChannels);
    };
    buffers[key] = buf;
    synth = Synth("loopRecord" ++ numChannels,
      [\in, bus, \buf, buf, \preLevel, min(layer, 1), \loop, max(0,layer-1)],
      target: RootNode(server), addAction: \addToTail);
    countdownTime = clock.nextTimeOnGrid(quant, -1*countdown);
    clock.schedAbs(countdownTime, {
      fork {
        countdown.do {|i|
          (countdown - i).postln;
          1.wait;
        };
      };
    });
    clock.schedAbs(countdownTime + countdown, {
      synth.set(\run, 1);
      "Recording".postln;
      fork {
        recordBeats.do {|i|
          var remaining = recordBeats - i;
          if (layer > 1) {
            var bar = ((remaining-1) / beats).asInteger + 1;
            var beat = ((remaining-1) % beats + 1);
            ("" ++ bar ++ ") " ++ beat).postln;
          } {
            remaining.postln;
          };
          1.wait;
        };
      };
    });
    clock.schedAbs(countdownTime + countdown + beats, {
      "Done".postln;
      if (playback) {
        this.play(key);
      };
      if (callback.notNil) {
        callback.value;
      };
    });
    if (layer > 1) {
      clock.schedAbs(countdownTime + countdown + recordBeats - 0.5, {
        synth.set(\loop, 0);
      });
    };
  }

  *free {|key| Looper.current.free(key)}
  free {|key|
    if (synths[key].notNil) {
      synths[key].free;
      synths[key] = nil;
    };
    buffers[key].free;
    buffers[key] = nil;
  }
}
