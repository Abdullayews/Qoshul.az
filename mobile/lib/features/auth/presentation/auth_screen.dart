import 'package:flutter/material.dart';

import '../../../core/theme/app_theme.dart';
import '../../../core/widgets/brand.dart';
import '../../../core/widgets/ui.dart';

/// Daxil ol / Qeydiyyat — web auth.tsx portu (UI mərhələsi: mock auth).
class AuthScreen extends StatefulWidget {
  final VoidCallback onSignedIn;
  const AuthScreen({super.key, required this.onSignedIn});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

enum _Mode { enter, up, forgot }

class _AuthScreenState extends State<AuthScreen> {
  _Mode mode = _Mode.enter;
  final emailCtrl = TextEditingController();
  final passCtrl = TextEditingController();
  final nameCtrl = TextEditingController();
  final repeatCtrl = TextEditingController();
  bool busy = false;
  String? error;
  String? msg;

  @override
  void dispose() {
    emailCtrl.dispose();
    passCtrl.dispose();
    nameCtrl.dispose();
    repeatCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      busy = true;
      error = null;
      msg = null;
    });
    await Future.delayed(const Duration(milliseconds: 800));
    if (!mounted) return;
    if (mode == _Mode.up) {
      if (passCtrl.text.length < 8) {
        setState(() {
          busy = false;
          error = 'Şifrə ən azı 8 simvol olmalıdır.';
        });
        return;
      }
      if (passCtrl.text != repeatCtrl.text) {
        setState(() {
          busy = false;
          error = 'Şifrələr eyni deyil.';
        });
        return;
      }
    }
    if (mode == _Mode.forgot) {
      setState(() {
        busy = false;
        msg = 'Şifrəni yeniləmək üçün link e-poçtuna göndərildi.';
      });
      return;
    }
    // Mock uğurlu giriş.
    setState(() => busy = false);
    widget.onSignedIn();
  }

  Future<void> _google() async {
    setState(() => busy = true);
    await Future.delayed(const Duration(milliseconds: 900));
    if (!mounted) return;
    setState(() => busy = false);
    widget.onSignedIn();
  }

  @override
  Widget build(BuildContext context) {
    final title = mode == _Mode.enter
        ? 'Xoş gəldin!'
        : mode == _Mode.up
            ? 'Hesab yarat'
            : 'Şifrəni unutmusan?';
    final subtitle = mode == _Mode.forgot
        ? 'E-poçtunu yaz, sənə yeniləmə linki göndərək.'
        : 'Yeni insanlara, yeni planlara qoşul.';

    return Scaffold(
      backgroundColor: Q.background,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 28),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.only(top: 24, bottom: 20),
                  child: Wordmark(fontSize: 27),
                ),
                Text(title, style: T.h1),
                const SizedBox(height: 4),
                Text(subtitle, style: T.subtitle),
                const SizedBox(height: 22),
                if (mode != _Mode.forgot) ...[
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: busy ? null : _google,
                      icon: const Icon(Icons.g_mobiledata, size: 22),
                      label: Text('Google ilə davam et', style: T.button),
                    ),
                  ),
                  const SizedBox(height: 14),
                  const Center(
                    child: Text('və ya',
                        style: TextStyle(
                            fontFamily: T.family,
                            fontSize: 11,
                            color: Q.mutedFg)),
                  ),
                  const SizedBox(height: 6),
                ],
                if (mode == _Mode.up)
                  FormFieldX(
                    label: 'Ad və soyad',
                    child: TextField(
                      controller: nameCtrl,
                      maxLength: 100,
                      style: T.input,
                      decoration: const InputDecoration(counterText: ''),
                    ),
                  ),
                FormFieldX(
                  label: 'E-poçt',
                  child: TextField(
                    controller: emailCtrl,
                    keyboardType: TextInputType.emailAddress,
                    style: T.input,
                  ),
                ),
                if (mode != _Mode.forgot)
                  FormFieldX(
                    label: 'Şifrə',
                    child: TextField(
                      controller: passCtrl,
                      obscureText: true,
                      style: T.input,
                    ),
                  ),
                if (mode == _Mode.up)
                  FormFieldX(
                    label: 'Şifrəni təkrarla',
                    child: TextField(
                      controller: repeatCtrl,
                      obscureText: true,
                      style: T.input,
                    ),
                  ),
                if (error != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(error!,
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 11,
                            color: Q.destructive)),
                  ),
                if (msg != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(msg!,
                        style: const TextStyle(
                            fontFamily: T.family,
                            fontSize: 11,
                            color: Q.mutedFg)),
                  ),
                const SizedBox(height: 8),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton(
                    onPressed: busy ? null : _submit,
                    child: busy
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Q.primaryFg,
                            ),
                          )
                        : Text(
                            mode == _Mode.enter
                                ? 'Daxil ol'
                                : mode == _Mode.up
                                    ? 'Qeydiyyatdan keç'
                                    : 'Link göndər',
                            style: T.button,
                          ),
                  ),
                ),
                const SizedBox(height: 16),
                Center(
                  child: Column(
                    children: [
                      if (mode == _Mode.enter) ...[
                        TextButton(
                          onPressed: () => setState(() => mode = _Mode.forgot),
                          child: const Text('Şifrəni unutmusan?',
                              style: TextStyle(
                                  fontFamily: T.family, fontSize: 12)),
                        ),
                        TextButton(
                          onPressed: () => setState(() => mode = _Mode.up),
                          child: const Text('Hesabın yoxdur? Qeydiyyatdan keç',
                              style: TextStyle(
                                  fontFamily: T.family, fontSize: 12)),
                        ),
                      ] else
                        TextButton(
                          onPressed: () => setState(() => mode = _Mode.enter),
                          child: const Text('Hesabın var? Daxil ol',
                              style: TextStyle(
                                  fontFamily: T.family, fontSize: 12)),
                        ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
