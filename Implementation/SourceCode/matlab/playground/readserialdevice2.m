instrfind
port = 'COM1';
Baud_Rate = 57600; %9600
Data_Bits = 8;
Stop_Bits = 1;
device = serial(port,'BaudRate',Baud_Rate,'DataBits',Data_Bits,'StopBits',Stop_Bits);
fopen(device);

BYTES_PACK = 17;
NPACKETS = 130;
NBYTESREAD = BYTES_PACK * NPACKETS;

data = zeros(NBYTESREAD, 1);

figure
hold on

for rep = 1:20
    for n = 1:NBYTESREAD
        data(n) = fread(device, 1);
    end

    parseddata = zeros(BYTES_PACK, NPACKETS);
    for n = 1:BYTES_PACK
        parseddata(n,:) = data(n:BYTES_PACK:end);
    end

    for n = 1:BYTES_PACK
        subplot(5,4,n);
        plot(parseddata(n, :));
        drawnow;
    end
    pause(0.05)
end

fclose(device);