import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../core/design/xo_assets.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_gradients.dart';
import '../../../../core/design/xo_radius.dart';
import '../../../../core/design/xo_shadows.dart';
import '../../../../core/design/xo_spacing.dart';
import 'xo_auth_header.dart';

/// 登录/注册统一脚手架，固定使用参考图的深青外景 + 浅色手机卡片风格。
class XoAuthScaffold extends StatelessWidget {
  const XoAuthScaffold({
    required this.title,
    required this.subtitle,
    required this.form,
    required this.bottomLink,
    super.key,
    this.isRegister = false,
    this.onBack,
  });

  final String title;
  final String subtitle;
  final Widget form;
  final Widget bottomLink;
  final bool isRegister;
  final VoidCallback? onBack;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: XoColors.deepTeal,
      body: DecoratedBox(
        decoration: const BoxDecoration(gradient: XoGradients.authBackground),
        child: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) {
              final desktopPreview = constraints.maxWidth > 520;
              final phoneWidth = math.min(430.0, constraints.maxWidth - 32);
              final phoneHeight = desktopPreview
                  ? math.min(
                      constraints.maxHeight - 32,
                      isRegister ? 780.0 : 760.0,
                    )
                  : constraints.maxHeight;

              return Center(
                child: SizedBox(
                  width: desktopPreview ? phoneWidth : constraints.maxWidth,
                  height: desktopPreview ? phoneHeight : constraints.maxHeight,
                  child: _AuthPhoneFrame(
                    desktopPreview: desktopPreview,
                    child: SingleChildScrollView(
                      padding: EdgeInsets.fromLTRB(
                        XoSpacing.pageHorizontal,
                        isRegister ? XoSpacing.md : XoSpacing.xl,
                        XoSpacing.pageHorizontal,
                        XoSpacing.lg,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (onBack != null)
                            IconButton(
                              onPressed: onBack,
                              icon: const Icon(Icons.arrow_back),
                              color: XoColors.textMain,
                              padding: EdgeInsets.zero,
                              alignment: Alignment.centerLeft,
                            ),
                          SizedBox(
                            height: onBack == null
                                ? XoSpacing.lg
                                : XoSpacing.sm,
                          ),
                          SizedBox(
                            height: isRegister ? 220 : 246,
                            child: Stack(
                              clipBehavior: Clip.none,
                              children: [
                                Positioned(
                                  left: 0,
                                  top: isRegister ? 24 : 22,
                                  child: XoAuthHeader(compact: isRegister),
                                ),
                                Positioned(
                                  right: isRegister ? -28 : -20,
                                  bottom: isRegister ? -4 : 0,
                                  child: _AuthHeroImage(isRegister: isRegister),
                                ),
                              ],
                            ),
                          ),
                          SizedBox(
                            height: isRegister ? XoSpacing.md : XoSpacing.lg,
                          ),
                          Text(
                            title,
                            style: const TextStyle(
                              color: XoColors.textMain,
                              fontSize: 30,
                              fontWeight: FontWeight.w800,
                              height: 1.12,
                            ),
                          ),
                          const SizedBox(height: XoSpacing.sm),
                          Text(
                            subtitle,
                            style: const TextStyle(
                              color: XoColors.textSecondary,
                              fontSize: 16,
                              height: 1.45,
                            ),
                          ),
                          const SizedBox(height: XoSpacing.xl),
                          _AuthFormCard(child: form),
                          const SizedBox(height: XoSpacing.lg),
                          Center(child: bottomLink),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

class _AuthPhoneFrame extends StatelessWidget {
  const _AuthPhoneFrame({required this.child, required this.desktopPreview});

  final Widget child;
  final bool desktopPreview;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: const Color(0xFFF6FBFA),
        borderRadius: BorderRadius.circular(desktopPreview ? 30 : 0),
        boxShadow: desktopPreview ? const [XoShadows.card] : null,
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(desktopPreview ? 30 : 0),
        child: Stack(
          children: [
            const Positioned(
              top: 0,
              left: 0,
              right: 0,
              height: 360,
              child: DecoratedBox(
                decoration: BoxDecoration(gradient: XoGradients.lightCard),
              ),
            ),
            child,
            if (desktopPreview)
              const Positioned(
                left: 145,
                right: 145,
                bottom: 16,
                child: _HomeIndicator(),
              ),
          ],
        ),
      ),
    );
  }
}

class _AuthHeroImage extends StatelessWidget {
  const _AuthHeroImage({required this.isRegister});

  final bool isRegister;

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: 0.94,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(XoRadius.xl),
        child: Image.asset(
          isRegister ? XoAssets.authRegisterHero : XoAssets.authLoginHero,
          width: isRegister ? 250 : 238,
          height: isRegister ? 214 : 210,
          fit: BoxFit.cover,
        ),
      ),
    );
  }
}

class _AuthFormCard extends StatelessWidget {
  const _AuthFormCard({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: const Color(0xFFE3EBE8)),
        boxShadow: const [XoShadows.card],
      ),
      child: Padding(
        padding: const EdgeInsets.all(XoSpacing.cardPadding),
        child: child,
      ),
    );
  }
}

class _HomeIndicator extends StatelessWidget {
  const _HomeIndicator();

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: const Color(0xFF111827),
        borderRadius: BorderRadius.circular(999),
      ),
      child: const SizedBox(height: 4),
    );
  }
}
