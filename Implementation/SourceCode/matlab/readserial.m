function [resultData] = readserial(port, durationSeconds)

if ~isempty(instrfind)
     fclose(instrfind);
     delete(instrfind);
end

Baud_Rate = 57600; %9600
Data_Bits = 8;
Stop_Bits = 1;
device = serial(port,'BaudRate',Baud_Rate,'DataBits',Data_Bits,'StopBits',Stop_Bits);
fopen(device);

COUNTER_POS = 4;

samplingrate = 256; %Hz
BYTES_PACK = 17;
NPACKETS = samplingrate * durationSeconds;
NBYTESREAD = BYTES_PACK * NPACKETS;
NELECTRODESSIGNALS = 6;

data = zeros(NBYTESREAD, 1);

t0 = clock;
%Read data
for n = 1:NBYTESREAD
    data(n) = fread(device, 1);
end
ms = round(etime(clock,t0) * 1000);

%Extract 17 plots
parseddata = zeros(BYTES_PACK, NPACKETS);
[rows, cols] = size(parseddata);
for n = 1:BYTES_PACK
    parseddata(n,:) = data(n:BYTES_PACK:end);
end

%Determine index of counter plot
counterIdx = -1;
for n = 1:BYTES_PACK
    lastVal = parseddata(n, 1);
    for idx = 2:NPACKETS
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
delta = COUNTER_POS - counterIdx;
if delta ~= 0
    if delta > 0 
        part1 = parseddata(1:rows - delta, :);
        part2 = parseddata(rows - delta + 1 : rows, :);
        parseddata = vertcat(part1, part2);
    else
        part2 = parseddata(1: -1 *delta, :);
        part1 = parseddata(-1 *delta + 1 : rows, :);
        parseddata = vertcat(part1, part2);
    end
end

%Read channel data
trace = zeros(NELECTRODESSIGNALS, cols);
lineID = [
    5, 6;
    7, 8;
    9, 10;
    11, 12;
    13, 14;
    15, 16];
for l = 1:NELECTRODESSIGNALS
    for m = 1:cols
      trace(l,m) = bin2dec(strcat(dec2bin(parseddata(lineID(l,1),m)),dec2bin(parseddata(lineID(l,2),m))))./1023;
    end
end

fclose(device);

resultData = trace;

end