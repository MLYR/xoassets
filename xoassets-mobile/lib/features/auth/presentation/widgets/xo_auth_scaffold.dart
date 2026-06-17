import 'package:flutter/material.dart';

import '../../../../core/design/xo_assets.dart';
import '../../../../core/design/xo_colors.dart';
import '../../../../core/design/xo_gradients.dart';
import '../../../../core/design/xo_radius.dart';
import '../../../../core/design/xo_spacing.dart';
import '../../../../core/widgets/xo_card.dart';
import 'xo_auth_header.dart';

/// 登录/注册统一脚手架，承接品牌区、金融科技插画、表单卡片和底部信任卖点。
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
    final topInset = MediaQuery.paddingOf(context).top;
    return Scaffold(
      backgroundColor: XoColors.pageBg,
      body: Stack(
        children: [
          Positioned(
            top: -120,
            left: -80,
            right: -80,
            height: 360,
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: XoGradients.lightCard,
                borderRadius: BorderRadius.circular(220),
              ),
            ),
          ),
          SafeArea(
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 430),
                child: SingleChildScrollView(
                  padding: EdgeInsets.fromLTRB(
                    XoSpacing.pageHorizontal,
                    isRegister ? XoSpacing.sm : XoSpacing.lg,
                    XoSpacing.pageHorizontal,
                    XoSpacing.lg + topInset * 0.1,
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
                      const SizedBox(height: XoSpacing.sm),
                      SizedBox(
                        height: isRegister ? 176 : 190,
                        child: Stack(
                          clipBehavior: Clip.none,
                          children: [
                            Positioned(
                              right: -8,
                              top: isRegister ? 0 : 8,
                              child: _AuthHeroImage(isRegister: isRegister),
                            ),
                            Positioned(
                              left: 0,
                              top: isRegister ? 34 : 44,
                              child: XoAuthHeader(compact: isRegister),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: XoSpacing.xl),
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
                      XoCard(
                        padding: const EdgeInsets.all(XoSpacing.cardPadding),
                        child: form,
                      ),
                      const SizedBox(height: XoSpacing.lg),
                      Center(child: bottomLink),
                      const SizedBox(height: XoSpacing.lg),
                      const _AuthTrustBar(),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
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
      opacity: 0.9,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(XoRadius.xl),
        child: Image.asset(
          isRegister ? XoAssets.authRegisterHero : XoAssets.authLoginHero,
          width: isRegister ? 166 : 150,
          height: isRegister ? 146 : 150,
          fit: BoxFit.cover,
        ),
      ),
    );
  }
}

class _AuthTrustBar extends StatelessWidget {
  const _AuthTrustBar();

  @override
  Widget build(BuildContext context) {
    return const Row(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: [
        _TrustItem(icon: Icons.verified_user_outlined, label: '安全可靠'),
        _TrustItem(icon: Icons.trending_up_outlined, label: '专业智能'),
        _TrustItem(icon: Icons.lock_outline, label: '隐私保护'),
      ],
    );
  }
}

class _TrustItem extends StatelessWidget {
  const _TrustItem({required this.icon, required this.label});

  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        DecoratedBox(
          decoration: BoxDecoration(
            color: XoColors.primaryLight,
            borderRadius: BorderRadius.circular(18),
          ),
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Icon(icon, size: 18, color: XoColors.primary),
          ),
        ),
        const SizedBox(height: XoSpacing.xs),
        Text(
          label,
          style: const TextStyle(color: XoColors.textSecondary, fontSize: 12),
        ),
      ],
    );
  }
}
