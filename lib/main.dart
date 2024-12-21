import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Mavi Işık Filtresi',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const BlueFilterPage(),
    );
  }
}

class BlueFilterPage extends StatefulWidget {
  const BlueFilterPage({super.key});

  @override
  State<BlueFilterPage> createState() => _BlueFilterPageState();
}

class _BlueFilterPageState extends State<BlueFilterPage> {
  static const platform = MethodChannel('com.example.blue_light_filter/filter');
  bool _isFilterActive = false;
  double _filterIntensity = 0.0;

  @override
  void initState() {
    super.initState();
    platform.setMethodCallHandler((call) async {
      if (call.method == "retryFilter") {
        await _toggleFilter();
      }
    });
  }

  Future<void> _toggleFilter() async {
    try {
      final bool result = await platform.invokeMethod('toggleFilter', {
        'intensity': _filterIntensity,
        'isActive': !_isFilterActive,
      });
      setState(() {
        _isFilterActive = result;
      });
    } on PlatformException catch (e) {
      print("Failed to toggle filter: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mavi Işık Filtresi'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              'Filtre ${_isFilterActive ? 'Aktif' : 'Kapalı'}',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: 20),
            Slider(
              value: _filterIntensity,
              onChanged: (value) {
                setState(() {
                  _filterIntensity = value;
                });
                if (_isFilterActive) {
                  _toggleFilter();
                }
              },
              min: 0.0,
              max: 1.0,
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: _toggleFilter,
              child: Text(_isFilterActive ? 'Filtreyi Kapat' : 'Filtreyi Aç'),
            ),
          ],
        ),
      ),
    );
  }
}
