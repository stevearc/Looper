+ Looper {
  loadSynthDefs {
    server.waitForBoot({
      (1..2).do { |numChannels|
        var panFunc;
        if (numChannels == 1) {
          panFunc = { |in, pan=0| Pan2.ar(in, pan) };
        } {
          panFunc = { |in, pan=0| Balance2.ar(in[0], in[1], pan) };
        };
        SynthDef("loopRecord" ++ numChannels, { |in=0, out=0, buf=0, run=0, recLevel=1, preLevel=0, loop=0|
          var sig = In.ar(in, numChannels);
          RecordBuf.ar(sig, buf, 0, recLevel, preLevel, run, loop, doneAction: Done.freeSelf);
        }).add;
        SynthDef("loopPlay" ++ numChannels,
          {|out=0, buf=0, amp=1.0, loop=0|
            var sig = PlayBuf.ar(numChannels, buf, BufRateScale.ir(buf),
              loop: loop, doneAction: Done.freeSelf);
            sig = SynthDef.wrap(panFunc,  prependArgs: [sig]);
            Out.ar(out, sig * amp);
        }).add;
      }
    });
  }
}
