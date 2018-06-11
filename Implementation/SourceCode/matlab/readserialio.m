function [resultData] = readserialio(portSpec, secondsToRead)

samplingRate = 256;
bytesInPack = 17;
estimatedSignalLength = samplingRate * secondsToRead;
baudRate = 57600;
InputBufferSize = 65536;
[h,e] = IOPort('OpenSerialPort', portSpec);
IOPort('ConfigureSerialPort', h, sprintf('BaudRate=%i', baudRate));
IOPort('ConfigureSerialPort', h, 'BlockingBackgroundRead=0');
IOPort('ConfigureSerialPort', h, sprintf('InputBufferSize=%i', InputBufferSize));
IOPort('ConfigureSerialPort', h, 'ReceiveLatency=0.0039');

%Start reading
beep on;
beep;

IOPort('Purge', h);
IOPort('ConfigureSerialPort', h, 'StartBackgroundRead=1');

%Wait and look at bytes read
WaitSecs(secondsToRead);
bytestoget = IOPort('BytesAvailable', h)
[longdata, when, e] = IOPort('Read', h, 1, bytestoget);

beep;
beep off;

%Stop reading
IOPort('ConfigureSerialPort', h, 'StopBackgroundRead');
IOPort('Purge', h);
IOPort('Close', h);

%Parse data
lenPerPlot = min(estimatedSignalLength, floor(length(longdata)/bytesInPack));
parseddata = zeros(bytesInPack, lenPerPlot);
for n = 1:bytesInPack
    currdata = longdata(n:bytesInPack:end);
    for m = 1:lenPerPlot
        parseddata(n, m) = currdata(m);
    end
end

%Determine index of counter plot
counterIdx = -1;
for n = 1:bytesInPack
    lastVal = parseddata(n, 1);
    for idx = 2:lenPerPlot
        currentVal = parseddata(n, idx);
        if ((currentVal == 0 && lastVal == 255) || (currentVal == (lastVal + 1)))
            counterIdx = n;
            lastVal = currentVal;
        else
            counterIdx = -1;
            break;
        end
    end
    
    if counterIdx ~= -1
        break;
    end
end

%Reposition data, so that counter plot is on counter pos
delta = 4 - counterIdx;
if delta ~= 0
    if delta > 0 
        part1 = parseddata(1:bytesInPack - delta, :);
        part2 = parseddata(bytesInPack - delta + 1 : bytesInPack, :);
        parseddata = vertcat(part1, part2);
    else
        part2 = parseddata(1: -1 *delta, :);
        part1 = parseddata(-1 *delta + 1 : bytesInPack, :);
        parseddata = vertcat(part1, part2);
    end
end

% figure;
% for n = 1:bytesInPack
%    subplot(5,4,n);
%    plot(parseddata(n, :));
% end

%Read channel data
trace = zeros(6, lenPerPlot);
lineID = [
    5, 6;
    7, 8;
    9, 10;
    11, 12;
    13, 14;
    15, 16];
for l = 1:6
    for m = 1:lenPerPlot
        %Extract most significant bit
        % msb = decimalToBinaryVector(parseddata(lineID(l,1), m), 8);
        msb = parseddata(lineID(l,1), m) * 256;
        
        %Clear error bits
        % for b = 1:6
        %     msb(b) = 0;
        % end
        
        %Extract least significant bit
        % lsb = decimalToBinaryVector(parseddata(lineID(l,2), m), 8);
        lsb = parseddata(lineID(l,2), m);
        
        %Create one data value of 10 bits
        % trace(l,m) = binaryVectorToDecimal(horzcat(msb, lsb)) / 1023;
        trace(l,m) = (msb + lsb) / 1023;
    end
end

figure;
for n = 1:6
   subplot(2,3,n);
   plot(trace(n, :));
end

resultData = trace;

end