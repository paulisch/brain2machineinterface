duration=10;
%Load serial data
data = dlmread('testdata/frontal_lobe_10s_teeth_1.txt');
samplingrate=256;
[rows, cols] = size(data);
k=0:samplingrate*duration-1;

sample = data(1, 1:end);
%sample = smooth(sample);
%sample = detrend(sample, 0);

%figure;
%plot(sample);

filteredData = bpfilt(sample, 1, 49, samplingrate, 0);
%sample = filteredData;
%sample = detrend(sample, 0);

figure;
plot(k./samplingrate, sample);

%Fourier
y=fft(sample);
yp=abs(y); %Amptlitudengang
yang=angle(y); %Phasengang
figure;
%stem(k./duration, yp./(duration*samplingrate/2));
stem(k./duration, yp);
%stem(yp);